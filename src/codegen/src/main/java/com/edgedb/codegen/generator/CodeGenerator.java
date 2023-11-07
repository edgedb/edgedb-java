package com.edgedb.codegen.generator;

import com.edgedb.codegen.generator.types.TypeGenerator;
import com.edgedb.codegen.generator.types.V1TypeGenerator;
import com.edgedb.codegen.generator.types.V2TypeGenerator;
import com.edgedb.driver.*;
import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.codecs.NullCodec;
import com.edgedb.driver.binary.codecs.ObjectCodec;
import com.edgedb.driver.binary.protocol.ParseResult;
import com.edgedb.driver.binary.protocol.ProtocolVersion;
import com.edgedb.driver.binary.protocol.QueryParameters;
import com.edgedb.driver.binary.protocol.common.Cardinality;
import com.edgedb.driver.binary.protocol.common.IOFormat;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.exceptions.EdgeDBErrorException;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.namingstrategies.NamingStrategy;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
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

    private static final Pattern HEADER_PATTERN = Pattern.compile("^// edgeql:([0-9a-fA-F]{64})$");
    private final EdgeDBClient client;
    private final Logger logger;

    public CodeGenerator(EdgeDBConnection connection, Logger logger) throws EdgeDBException {
        this.client = new EdgeDBClient(connection, EdgeDBClientConfig.builder()
                .withExplicitObjectIds(false)
                .build()
        );
        this.logger = logger;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public CompletionStage<Void> generate(
            List<String> edgeqlFiles, GeneratorContext context, @Nullable TypeGenerator typeGenerator, boolean force
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

        if(targetInfos.isEmpty()) {
            logger.info("Found no files to generate from");
            return CompletableFuture.completedFuture(null);
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
        final var target = iter.next();
        return generate(target, context, generator, force, false)
                .thenCompose(v -> {
                    if(!v) {
                        logger.warn("Got unsuccessful generation result for file {}, skipping...", target.path);
                    } else {
                        logger.info("Generation complete for {}", target.path);
                    }

                    if(iter.hasNext()) {
                        return generate0(iter, context, generator, force);
                    }

                    return CompletableFuture.completedFuture(null);
                });
    }

    public CompletionStage<Boolean> generate(
            GeneratorTargetInfo target, GeneratorContext context, @Nullable TypeGenerator typeGenerator,
            boolean force
    ) {
       return generate(target, context, typeGenerator, force, true);
    }

    private CompletionStage<Boolean> generate(
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
                                        try {
                                            generator.postProcess(context);
                                        } catch (IOException e) {
                                            throw new CompletionException(e);
                                        }
                                    }

                                    return v;
                                })
                );
    }

    private CompletionStage<Boolean> validateOrGenerate(
            GeneratorTargetInfo target, GeneratorContext context, TypeGenerator typeGenerator, boolean force
    ) {
        var outputPath = target.getGenerationPath(context);

        if(!force && isUpToDate(target, context)) {
            logger.info("Skipping {}: up to date version exists: {}", target.path, outputPath);
            return CompletableFuture.completedFuture(null);
        }

        logger.info("Parsing {}...", target.path);

        return parse(target)
                .exceptionally(err -> {
                    if(err.getCause() instanceof EdgeDBErrorException) {
                        var formatted = ((EdgeDBErrorException) err.getCause()).toString();
                        logger.error("Failed to parse {}\n{}",target.path, formatted);
                    } else {
                        logger.error("Failed to parse {}", target.path, err);
                    }

                    return null;
                })
                .thenApply(parseResult -> {
                    if(parseResult == null) {
                        logger.debug("Skipping post-parse");
                        return null;
                    }

                    try {
                        return Map.entry(typeGenerator.getType(parseResult.outCodec, target, context), parseResult);
                    } catch (IOException e) {
                        logger.error("Failed to generated types for {}", target.path, e);
                        return null;
                    }
                })
                .thenApply(result -> {
                    if(result == null) {
                        logger.debug("Skipping post type generation");
                        return null;
                    }

                    try {
                        return generate(result.getKey(), result.getValue(), context, typeGenerator, target);
                    } catch (IOException e) {
                        logger.error("Failed to generated source file for {}", target.path, e);
                        return null;
                    }
                })
                .thenApply(file -> {
                    if(file == null) {
                        logger.debug("Skipping post generation");
                        return false;
                    }

                    var targetOutputPath = context.outputDirectory.resolve(target.filename + ".java");

                    try(var fileStream = Files.newBufferedWriter(targetOutputPath)) {
                        file.writeTo(fileStream);
                        fileStream.flush();
                        logger.info("{} -> {}", target.path, targetOutputPath);
                        return true;
                    } catch (IOException e) {
                        logger.error("Failed to write generated file to the directory {}", context.outputDirectory, e);
                        return false;
                    }
                });
    }

    private JavaFile generate(
            TypeName resultType, ParseResult parseResult, GeneratorContext context, TypeGenerator typeGenerator,
            GeneratorTargetInfo target
    ) throws IOException {
        var method = "query";

        TypeName methodReturnType;
        switch (parseResult.cardinality) {
            case NO_RESULT:
                methodReturnType = TypeName.VOID;
                method = "execute";
                break;
            case AT_MOST_ONE:
                methodReturnType = resultType.annotated(AnnotationSpec.builder(Nullable.class).build());
                method = "querySingle";
                break;
            case ONE:
                method = "queryRequiredSingle";
                methodReturnType = resultType;
                break;
            default:
                methodReturnType = ParameterizedTypeName.get(ClassName.get(List.class), resultType);
                break;
        }

        var parameters = buildArguments(parseResult.inCodec, typeGenerator, context);

        var codeblockBuilder = CodeBlock.builder()
                .indent()
                .add("return client.$L(\n$T.class, \nQUERY, \n", method, resultType);

        if(!parameters.isEmpty()) {
            codeblockBuilder
                    .add("new $T<>(){{\n", HashMap.class)
                    .indent();

            var args = parameters.entrySet()
                    .stream()
                    .map(v -> "put(\"" + v.getKey() + "\", " + v.getValue().name + ");")
                    .collect(Collectors.toList());


            for(var i = 0; i != args.size(); i++) {
                var arg = args.get(i);

                codeblockBuilder.add(arg + "\n");
            }

            codeblockBuilder.unindent().add("}}, \n");
        }

        codeblockBuilder.add("$T.of(\n", EnumSet.class).indent();

        var capabilities = parseResult.capabilities.stream().map(v -> "$T." + v).collect(Collectors.toList());

        for (var i = 0; i != capabilities.size(); i++) {
            var capability = capabilities.get(i);

            if(i != capabilities.size() - 1) {
                capability += ",";
            }

            codeblockBuilder.add(capability + "\n", Capabilities.class);
        }

        codeblockBuilder.unindent().add(")\n").unindent().add(")");

        var methodJavaDoc = CodeBlock.builder()
                .add(
                        "Executes the query defined in the file {@code $L.edgeql} with the capabilities {@code $L}.\n" +
                        "The query:\n" +
                        "<pre>\n" +
                        "{@literal $L}" +
                        "</pre>",
                        target.filename,
                        parseResult.capabilities
                                .stream()
                                .map(v ->
                                        v.name().toLowerCase(Locale.ROOT).replace("_", " ")
                                )
                                .collect(Collectors.joining(", ")),
                        target.edgeql
                );

        if(resultType instanceof ClassName && ((ClassName)resultType).canonicalName().startsWith(context.packageName)) {
            methodJavaDoc.add(
                    "\nThe result of the query is represented as the generated class {@linkplain $T}",
                    resultType
            );
        }

        methodJavaDoc.add(
                "\n@return A {@linkplain $1T} that represents the asynchronous operation of executing the " +
                "query and \nparsing the result. The {@linkplain $1T} result is {@linkplain $2T}.",
                CompletionStage.class, resultType
        );

        var runMethod = MethodSpec.methodBuilder("run")
                .addJavadoc(methodJavaDoc.build())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(EdgeDBQueryable.class, "client").build())
                .returns(ParameterizedTypeName.get(ClassName.get(CompletionStage.class), methodReturnType))
                .addStatement(codeblockBuilder.build());

        for (var parameter : parameters.values()) {
            runMethod.addParameter(parameter);
        }

        var classJavadoc = CodeBlock.builder()
                .add(
                        "A class containing the generated code responsible for the edgeql file {@code $L.edgeql}.<br/>\n" +
                        "Generated on: {@code $L}<br/>\n" +
                        "Edgeql hash: {@code $L}",
                        target.filename,
                        OffsetDateTime.now().toString(),
                        target.hash
                );

        if(resultType instanceof ClassName) {
            classJavadoc.add("\n@see $T", resultType);
        }

        var classSpec = TypeSpec.classBuilder(target.filename)
                .addJavadoc(classJavadoc.build())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(FieldSpec.builder(
                        TypeName.get(String.class),
                        "QUERY",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer(CodeBlock.of("$S", target.edgeql))
                        .build()
                )
                .addMethod(runMethod.build())
                .build();

        return JavaFile.builder(context.packageName, classSpec).build();
    }

    private Map<String, ParameterSpec> buildArguments(
            Codec<?> inCodec, TypeGenerator typeGenerator, GeneratorContext context
    ) throws IOException {
        if(inCodec instanceof NullCodec) {
            return Collections.emptyMap();
        }

        if(!(inCodec instanceof ObjectCodec)) {
            throw new UnsupportedOperationException(
                    "Expected object codec for arguments, but got " + inCodec.getClass().getName()
            );
        }

        var parameterSpecs = new HashMap<String, ParameterSpec>();

        var argumentCodec = (ObjectCodec)inCodec;

        // sort by cardinality
        var orderedProps = Arrays.stream(argumentCodec.elements).sorted((a, b)
                -> CARDINALITY_ORDINAL_MAP.get(b.cardinality) - CARDINALITY_ORDINAL_MAP.get(a.cardinality)
        ).collect(Collectors.toList());

        for (var property : orderedProps) {
            var parameter = ParameterSpec.builder(typeGenerator.getType(property.codec, null, context), NamingStrategy.camelCase().convert(property.name));

            if(property.cardinality == Cardinality.AT_MOST_ONE) {
                parameter.addAnnotation(Nullable.class);
            }

            parameterSpecs.put(property.name, parameter.build());
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
                })
                .thenApply(v -> {
                    logger.info("Choosing {} generator for result types", v);
                    return v;
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
