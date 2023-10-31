package com.edgedb.codegen.utils;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.namingstrategies.NamingStrategy;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.Collection;

public class GenerationUtils {
    public static TypeSpec generateDataClass(Collection<FieldSpec> fields, String typeName) {
        var typeSpec = TypeSpec.classBuilder(typeName)
                .addAnnotation(EdgeDBType.class)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC);

        var ctor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(EdgeDBDeserializer.class);

        for(var field : fields) {
            typeSpec.addField(field);
            typeSpec.addMethod(
                    MethodSpec.methodBuilder("get" + NamingStrategy.pascalCase().convert(field.name))
                            .returns(field.type)
                            .addModifiers(Modifier.PUBLIC)
                            .addStatement(CodeBlock.of("return this.$N", field))
                            .build()
            );

            ctor.addParameter(ParameterSpec.builder(field.type, field.name)
                    .addAnnotation(AnnotationSpec.builder(EdgeDBName.class)
                            .addMember("value", CodeBlock.of("$S", field.name)).build()
                    ).build()
            );

            ctor.addStatement(CodeBlock.of("this.$N = $N", field, field));
        }

        return typeSpec.addMethod(ctor.build()).build();
    }
}
