package com.edgedb.codegen.utils;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.namingstrategies.NamingStrategy;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class GenerationUtils {
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static TypeSpec generateDataClass(Collection<FieldSpec> fields, String typeName, boolean addGetters) {
        return generateDataClassBuilder(fields, typeName, addGetters).build();
    }

    public static TypeSpec.Builder generateDataClassBuilder(Collection<FieldSpec> fields, String typeName, boolean addGetters) {
        var typeSpec = TypeSpec.classBuilder(typeName)
                .addAnnotation(EdgeDBType.class)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC);

        var ctor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(EdgeDBDeserializer.class);

        for(var field : fields) {
            typeSpec.addField(field);
            if(addGetters) {
                typeSpec.addMethod(
                        MethodSpec.methodBuilder("get" + NamingStrategy.pascalCase().convert(field.name))
                                .returns(field.type)
                                .addModifiers(Modifier.PUBLIC)
                                .addStatement(CodeBlock.of("return this.$N", field))
                                .build()
                );
            }

            ctor.addParameter(ParameterSpec.builder(field.type, field.name)
                    .addAnnotation(AnnotationSpec.builder(EdgeDBName.class)
                            .addMember("value", CodeBlock.of("$S", field.name)).build()
                    ).build()
            );

            ctor.addStatement(CodeBlock.of("this.$N = $N", field, field));
        }

        typeSpec.addMethod(ctor.build());

        return typeSpec;
    }
}
