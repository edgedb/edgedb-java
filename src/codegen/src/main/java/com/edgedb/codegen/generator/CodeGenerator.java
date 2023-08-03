package com.edgedb.codegen.generator;

import com.edgedb.codegen.generator.types.TypeGenerator;
import com.edgedb.codegen.generator.types.V1TypeGenerator;
import com.edgedb.codegen.generator.types.V2TypeGenerator;
import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBClient;
import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.codecs.NullCodec;
import com.edgedb.driver.binary.codecs.ObjectCodec;
import com.edgedb.driver.binary.protocol.ParseResult;
import com.edgedb.driver.binary.protocol.ProtocolVersion;
import com.edgedb.driver.binary.protocol.QueryParameters;
import com.edgedb.driver.binary.protocol.common.Cardinality;
import com.edgedb.driver.binary.protocol.common.IOFormat;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CodeGenerator {
    private static final Map<Cardinality, Integer> CARDINALITY_ORDINAL_MAP = new HashMap<>(){{
        put(Cardinality.NO_RESULT, 0);
        put(Cardinality.AT_MOST_ONE, 1);
        put(Cardinality.ONE, 2);
        put(Cardinality.AT_LEAST_ONE, 3);
        put(Cardinality.MANY, 4);
    }};

    private static final Logger logger = LoggerFactory.getLogger(CodeGenerator.class);
    private static final Pattern HEADER_PATTERN = Pattern.compile("^// edgeql:([0-9a-fA-F]{64})$");
    private final EdgeDBClient client;

    public CodeGenerator(EdgeDBConnection connection) throws EdgeDBException {
        this.client = new EdgeDBClient(connection);
    }

    public CompletionStage<Void> generate(
            String[] edgeqlFiles, GeneratorContext context, @Nullable TypeGenerator typeGenerator, boolean force
    ) {
        var targetInfos = new ArrayList<GeneratorTargetInfo>();

        for (var target : edgeqlFiles) {
            try {
                targetInfos.add(GeneratorTargetInfo.fromFile(Path.of(target)));
            } catch (IOException | NoSuchAlgorithmException e) {
                logger.error("Failed to construct target info for {}", target, e);
                return CompletableFuture.failedFuture(e);
            }
        }

        var iter = targetInfos.iterator();

        return useOrInitTypeGenerator(
                typeGenerator,
                context,
                true,
                generator -> generate0(iter, context, generator, force)
        );
    }

    private CompletionStage<Void> generate0(
            Iterator<GeneratorTargetInfo> iter, GeneratorContext context, TypeGenerator generator, boolean force
    ) {
        return generate(iter.next(), context, generator, force, false)
                .thenCompose(v -> {
                    if(iter.hasNext()) {
                        return generate0(iter, context, generator, force);
                    }

                    return CompletableFuture.completedFuture(null);
                });
    }

    public CompletionStage<Void> generate(
            GeneratorTargetInfo target, GeneratorContext context, @Nullable TypeGenerator typeGenerator,
            boolean force
    ) {
       return generate(target, context, typeGenerator, force, true);
    }

    private CompletionStage<Void> generate(
            GeneratorTargetInfo target, GeneratorContext context, @Nullable TypeGenerator typeGenerator,
            boolean force, boolean postprocess
    ) {
        return useOrInitTypeGenerator(
                typeGenerator, context, postprocess,
                generator -> validateOrGenerate(target, context, generator, force)
        );
    }

    private <T> CompletionStage<T> useOrInitTypeGenerator(
            @Nullable TypeGenerator typeGenerator, GeneratorContext context, boolean postprocess,
            Function<TypeGenerator, CompletionStage<T>> function
    ) {
        return CompletableFuture.supplyAsync(() -> typeGenerator)
                .thenCompose(generator -> generator == null
                        ? getTypeGenerator()
                        : CompletableFuture.completedFuture(generator))
                .thenCompose(generator -> function
                                .apply(generator)
                                .thenApply(v -> {
                                    if(postprocess) {
                                        generator.postProcess(context);
                                    }

                                    return v;
                                })
                );
    }

    private CompletionStage<Void> validateOrGenerate(
            GeneratorTargetInfo target, GeneratorContext context, TypeGenerator typeGenerator, boolean force
    ) {
        var outputPath = target.getGenerationPath(context);

        if(!force && isUpToDate(target, context)) {
            logger.info("Skipping {}: up to date version exists: {}", target.path, outputPath);
            return CompletableFuture.completedFuture(null);
        }

        logger.info("Parsing {}...", target.path);

        return parse(target)
                .thenApply(parseResult -> {
                    try {
                        return Map.entry(typeGenerator.getType(parseResult.outCodec, target, context), parseResult);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                })
                .thenApply(result ->
                        {
                            try {
                                return generate(result.getKey(), result.getValue(), context, typeGenerator, target);
                            } catch (IOException e) {
                                throw new CompletionException(e);
                            }
                        }
                )
                .thenAccept(file -> {
                    try {
                        file.writeTo(context.outputDirectory.resolve(target.filename + ".g.java"));
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                });
    }

    private JavaFile generate(
            TypeName resultType, ParseResult parseResult, GeneratorContext context, TypeGenerator typeGenerator,
            GeneratorTargetInfo target
    ) throws IOException {
        var method = "query";

        switch (parseResult.cardinality) {
            case NO_RESULT:
                resultType = TypeName.VOID;
                method = "execute";
                break;
            case AT_MOST_ONE:
                resultType = resultType.annotated(AnnotationSpec.builder(Nullable.class).build());
                method = "querySingle";
                break;
            case ONE:
                method = "queryRequiredSingle";
                break;
            default:
                resultType = ParameterizedTypeName.get(ClassName.get(List.class), resultType);
                break;
        }

        var parameters = buildArguments(parseResult.inCodec, typeGenerator, context);

        var code = "return client.&L(QUERY, ";

        if(!parameters.isEmpty()) {
            code += "new HashMap<>(){{";
            code += parameters.stream()
                    .map(v -> "put(\"" + v.name + "\", " + v.name + ");").collect(Collectors.joining("; "));
            code += "}}, ";
        }

        code += "EnumSet<Capabilities>.of("
                + parseResult.capabilities.stream().map(v -> "Capabilities." + v).collect(Collectors.joining(", "))
                + ")";

        var runMethod = MethodSpec.methodBuilder("run")
                .addParameter(ParameterSpec.builder(EdgeDBQueryable.class, "client").build())
                .returns(resultType)
                .addStatement(CodeBlock.of(code, method));

        for (var parameter : parameters) {
            runMethod.addParameter(parameter);
        }


        var classSpec = TypeSpec.classBuilder(target.filename)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(FieldSpec.builder(
                        TypeName.get(String.class),
                        "QUERY",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer(CodeBlock.of("&S", target.edgeql))
                        .build()
                )
                .addMethod(runMethod.build())
                .build();

        return JavaFile.builder(context.packageName, classSpec).build();
    }

    private Collection<ParameterSpec> buildArguments(
            Codec<?> inCodec, TypeGenerator typeGenerator, GeneratorContext context
    ) throws IOException {
        if(inCodec instanceof NullCodec) {
            return Collections.emptyList();
        }

        if(!(inCodec instanceof ObjectCodec)) {
            throw new UnsupportedOperationException(
                    "Expected object codec for arguments, but got " + inCodec.getClass().getName()
            );
        }

        var parameterSpecs = new ArrayList<ParameterSpec>();

        var argumentCodec = (ObjectCodec)inCodec;

        // sort by cardinality
        var orderedProps = Arrays.stream(argumentCodec.elements).sorted((a, b)
                -> CARDINALITY_ORDINAL_MAP.get(b.cardinality) - CARDINALITY_ORDINAL_MAP.get(a.cardinality)
        ).collect(Collectors.toList());

        for (var property : orderedProps) {
            var parameter = ParameterSpec.builder(typeGenerator.getType(property.codec, null, context), property.name);

            if(property.cardinality == Cardinality.AT_MOST_ONE) {
                parameter.addAnnotation(Nullable.class);
            }

            parameterSpecs.add(parameter.build());
        }

        return parameterSpecs;
    }

    private CompletionStage<TypeGenerator> getTypeGenerator() {
        return getProtocolVersion()
                .thenApply(version -> {
                    switch (version.major) {
                        case 1:
                            return new V1TypeGenerator();
                        case 2:
                            return new V2TypeGenerator();
                        default:
                            throw new CompletionException(
                                    new EdgeDBException("Unsupported protocol version " + version)
                            );
                    }
                });
    }

    private CompletionStage<ProtocolVersion> getProtocolVersion() {
        return client.getClient(EdgeDBBinaryClient.class)
                .thenCompose(client -> client.ensureConnected().thenApply(v -> client))
                .thenApply(client -> client.getProtocolProvider().getVersion());
    }

    private CompletionStage<ParseResult> parse(GeneratorTargetInfo target) {
        return client.getClient(EdgeDBBinaryClient.class)
                .thenCompose(client -> client.getProtocolProvider().sendSyncMessage().thenApply(v -> client))
                .thenCompose(client ->
                    client.getProtocolProvider().parseQuery(new QueryParameters(
                            target.edgeql,
                            null,
                            EnumSet.allOf(Capabilities.class),
                            Cardinality.MANY,
                            IOFormat.BINARY,
                            false
                    ))
                );
    }

    private boolean isUpToDate(GeneratorTargetInfo target, GeneratorContext context) {
        if(!Files.exists(target.getGenerationPath(context))) {
            return false;
        }

        try(var reader = Files.newBufferedReader(target.path)) {
            var hashLine = reader.readLine();

            var match = HEADER_PATTERN.matcher(hashLine);

            return match.matches() && match.group(1).equals(target.hash);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
