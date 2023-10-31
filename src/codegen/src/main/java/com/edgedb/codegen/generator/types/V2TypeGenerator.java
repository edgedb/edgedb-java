package com.edgedb.codegen.generator.types;

import com.edgedb.codegen.generator.GeneratorContext;
import com.edgedb.codegen.generator.GeneratorTargetInfo;
import com.edgedb.codegen.utils.GenerationUtils;
import com.edgedb.codegen.utils.TextUtils;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.binary.codecs.*;
import com.edgedb.driver.binary.codecs.scalars.ScalarCodec;
import com.edgedb.driver.datatypes.Range;
import com.edgedb.driver.namingstrategies.NamingStrategy;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class V2TypeGenerator implements TypeGenerator {
    private final ParserMap parsers = new ParserMap() {{
        define(SparseObjectCodec.class, V2TypeGenerator.this::parseSparseObjectCodec);
        define(ObjectCodec.class, V2TypeGenerator.this::parseObjectCodec);
        define(TupleCodec.class, V2TypeGenerator.this::parseTupleCodec);
        define(ArrayCodec.class, V2TypeGenerator.this::parseArrayCodec);
        define(RangeCodec.class, V2TypeGenerator.this::parseRangeCodec);
        define(SetCodec.class, V2TypeGenerator.this::parseSetCodec);
        define(ScalarCodec.class, V2TypeGenerator.this::parseScalarCodec);
        define(NullCodec.class, V2TypeGenerator.this::parseNullCodec);
    }};

    @Override
    public TypeName getType(Codec<?> codec, @Nullable GeneratorTargetInfo target, GeneratorContext context) {
        var result = parsers.parse(codec, target, context);

        if(result != null) {
            return result;
        }

        throw new IllegalArgumentException("Unknown parse path for codec " + codec);
    }

    private TypeName parseNullCodec(NullCodec nullCodec, GeneratorTargetInfo target, GeneratorContext context) {
        return TypeName.OBJECT;
    }

    private TypeName parseScalarCodec(ScalarCodec<?> scalarCodec, GeneratorTargetInfo target, GeneratorContext context) {
        return TypeName.get(scalarCodec.getConvertingClass());
    }

    private TypeName parseSetCodec(SetCodec<?> setCodec, GeneratorTargetInfo target, GeneratorContext context) {
        return ParameterizedTypeName.get(ClassName.get(List.class), getType(setCodec.innerCodec, target, context));
    }

    private TypeName parseRangeCodec(RangeCodec<?> rangeCodec, GeneratorTargetInfo target, GeneratorContext context) {
        return ParameterizedTypeName.get(ClassName.get(Range.class), getType(rangeCodec.innerCodec, target, context));
    }

    private TypeName parseArrayCodec(ArrayCodec<?> arrayCodec, GeneratorTargetInfo target, GeneratorContext context) {
        return ArrayTypeName.of(getType(arrayCodec.innerCodec, target, context));
    }

    private TypeName parseTupleCodec(TupleCodec tupleCodec, GeneratorTargetInfo target, GeneratorContext context) {
        return ClassName.get("com.edgedb.driver.datatypes", "Tuple");
    }

    private TypeName parseSparseObjectCodec(SparseObjectCodec codec, @Nullable GeneratorTargetInfo target, GeneratorContext context) {
        throw new UnsupportedOperationException("Cannot parse sparse object codec");
    }

    private TypeName parseObjectCodec(ObjectCodec codec, @Nullable GeneratorTargetInfo target, GeneratorContext context) throws IOException {
        var fields = Arrays.stream(codec.elements)
                .map(x -> Map.entry(x, getType(x.codec, target, context)))
                .map(x -> FieldSpec.builder(
                        x.getValue(),
                        NamingStrategy.camelCase().convert(x.getKey().name),
                        Modifier.FINAL, Modifier.PRIVATE
                    ).addAnnotation(AnnotationSpec.builder(EdgeDBName.class)
                        .addMember("value", CodeBlock.of("$S", x.getKey().name)).build()
                    ).build()
                )
                .collect(Collectors.toList());

        var typeSpec = GenerationUtils.generateDataClass(fields, getTypeName(codec, target, context));

        var jFile = JavaFile.builder(context.packageName + ".results", typeSpec).build();

        try(var writer = Files.newBufferedWriter(context.outputDirectory.resolve(Path.of("results", typeSpec.name + ".java")))) {
            jFile.writeTo(writer);
            writer.flush();
        }

        return ClassName.get(context.packageName + ".results", typeSpec.name);
    }

    private String getTypeName(ObjectCodec codec, @Nullable GeneratorTargetInfo target, GeneratorContext context) {
        return getTypeName(codec, null, target, context);
    }

    private String getTypeName(ObjectCodec codec, @Nullable String name, @Nullable GeneratorTargetInfo target, GeneratorContext context) {
        var prefix = target != null
                ? target.filename
                : "";

        if(name != null) {
            return prefix + name + (codec.metadata != null ? TextUtils.nameWithoutModule(codec.metadata.schemaName) : "Result");
        } else {
            return prefix += codec.metadata != null ? TextUtils.nameWithoutModule(codec.metadata.schemaName) : "Result";
        }
    }

    @Override
    public void postProcess(GeneratorContext context) {

    }
}
