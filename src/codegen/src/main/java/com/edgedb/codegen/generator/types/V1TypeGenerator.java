package com.edgedb.codegen.generator.types;

import com.edgedb.codegen.generator.GeneratorContext;
import com.edgedb.codegen.generator.GeneratorTargetInfo;
import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.binary.codecs.*;
import com.edgedb.driver.binary.codecs.scalars.ScalarCodec;
import com.edgedb.driver.datatypes.Range;
import com.edgedb.driver.namingstrategies.NamingStrategy;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class V1TypeGenerator implements TypeGenerator {
    private static final Logger logger = LoggerFactory.getLogger(V1TypeGenerator.class);

    private int count;

    @Override
    public TypeName getType(Codec<?> codec, @Nullable GeneratorTargetInfo target, GeneratorContext context) throws IOException {
        var rootName = target == null
                ? codec.hashCode() + "Result"
                : target.filename + "Result";

        return getType(codec, rootName, target == null ? null : target.path, context);
    }

    private TypeName getType(Codec<?> codec, @Nullable String name, @Nullable Path workingFile, GeneratorContext context) throws IOException {
        if(codec instanceof SparseObjectCodec) {
            throw new UnsupportedOperationException("Cannot parse sparse object codec");
        } else if(codec instanceof ObjectCodec) {
            return generateResultType((ObjectCodec) codec, name, workingFile, context);
        } else if(codec instanceof TupleCodec) {
            return ClassName.get("com.edgedb.driver.datatypes", "Tuple");
        } else if(codec instanceof ArrayCodec<?>) {
            var arrayCodec = (ArrayCodec<?>)codec;
            return ArrayTypeName.of(getType(arrayCodec.innerCodec, name, workingFile, context));
        } else if(codec instanceof RangeCodec<?>) {
            var rangeCodec = (RangeCodec<?>)codec;
            return ParameterizedTypeName.get(ClassName.get(Range.class), getType(rangeCodec.innerCodec, name, workingFile, context));
        } else if(codec instanceof SetCodec<?>) {
            var setCodec = (SetCodec<?>)codec;
            return ParameterizedTypeName.get(ClassName.get(List.class), getType(setCodec.innerCodec, name, workingFile, context));
        } else if(codec instanceof ScalarCodec<?>) {
            var scalarCodec = (ScalarCodec<?>)codec;
            return TypeName.get(scalarCodec.getConvertingClass());
        } else if (codec instanceof NullCodec) {
            return TypeName.OBJECT;
        }

        throw new UnsupportedOperationException("Unknown type parse path for the codec " + codec.getClass());
    }

    private TypeName generateResultType(ObjectCodec object, @Nullable String name, @Nullable Path workingFile, GeneratorContext context) throws IOException {
        var typeName = name == null ? getSubtypeName(null) : name;
        var typeSpec = TypeSpec.classBuilder(typeName)
                .addAnnotation(EdgeDBType.class)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC);

        var fields = new ArrayList<FieldSpec>();

        for (var property : object.elements) {
            var fieldSpec = FieldSpec.builder(
                    getType(property.codec, property.name, workingFile, context),
                    NamingStrategy.camelCase().convert(property.name),
                    Modifier.FINAL, Modifier.PRIVATE
            ).addAnnotation(AnnotationSpec.builder(EdgeDBName.class)
                    .addMember("value", CodeBlock.of("$S", property.name)).build()
            ).build();

            typeSpec.addField(fieldSpec);
            fields.add(fieldSpec);

            typeSpec.addMethod(
                    MethodSpec.methodBuilder("get" + NamingStrategy.pascalCase().convert(property.name))
                            .returns(fieldSpec.type)
                            .addModifiers(Modifier.PUBLIC)
                            .addStatement(CodeBlock.of("return this.$N", fieldSpec))
                            .build()
            );
        }

        var ctor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(EdgeDBDeserializer.class);

        for (var field : fields) {
            ctor.addParameter(ParameterSpec.builder(field.type, field.name)
                    .addAnnotation(AnnotationSpec.builder(EdgeDBName.class)
                            .addMember("value", CodeBlock.of("$S", field.name)).build()
                    ).build()
            );

            ctor.addStatement(CodeBlock.of("this.$N = $N", field, field));
        }

        var type = typeSpec.addMethod(ctor.build()).build();

        var jFile = JavaFile.builder(context.packageName + ".results", type).build();

        logger.debug("Output directory creation status: {}", context.outputDirectory.resolve("results").toFile().mkdir());

        try(var writer = Files.newBufferedWriter(context.outputDirectory.resolve(Path.of("results", typeName + ".java")))) {
            jFile.writeTo(writer);
            writer.flush();
        }

        return ClassName.get(context.packageName + ".results", type.name);
    }

    @Override
    public void postProcess(GeneratorContext context) {

    }

    private String getSubtypeName(@Nullable String name) {
        return name == null
                ? "GenericSubType" + count++
                : name + "SubType" + count++;
    }
}
