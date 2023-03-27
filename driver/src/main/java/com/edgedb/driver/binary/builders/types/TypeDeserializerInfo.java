package com.edgedb.driver.binary.builders.types;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBIgnore;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.binary.builders.ObjectEnumerator;
import com.edgedb.driver.binary.builders.TypeDeserializerFactory;
import com.edgedb.driver.util.FastInverseIndexer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class TypeDeserializerInfo<T> {
    public final Map<String, FieldInfo> validFields;
    public final TypeDeserializerFactory<T> factory;

    private final Class<T> type;
    private final List<Method> setterMethods;
    private final Method[] methods;

    public TypeDeserializerInfo(Class<T> type) {
        this.type = type;
        this.methods = type.getDeclaredMethods();
        this.setterMethods = Arrays.stream(methods).filter((v) -> v.getName().startsWith("set")).collect(Collectors.toList());

        var fields = type.getDeclaredFields();

        this.validFields = new HashMap<>();

        for (var field: fields) {
            if(isValidField(field)) {
                validFields.put(new FieldInfo(field));
            }
        }

        this.validFields = validFields;

        this.factory = createFactory();
    }

    public boolean isValidField(Field field) {
        return field.getAnnotation(EdgeDBIgnore.class) == null;
    }

    private TypeDeserializerFactory<T> createFactory() {
        // check for constructor deserializer
        var constructors = this.type.getDeclaredConstructors();

        var ctorDeserializer = Arrays.stream(constructors).filter(x -> x.getAnnotation(EdgeDBDeserializer.class) != null).findFirst();

        if(ctorDeserializer.isPresent()) {
            var ctor = ctorDeserializer.get();
            var ctorParamsKVP = Arrays.stream(ctor.getParameters())
                    .map(v -> {
                        var edbName = v.getAnnotation(EdgeDBName.class);
                        return Map.entry(
                                edbName == null
                                    ? v.getName()
                                    : edbName.name() == null
                                        ? v.getName()
                                        : edbName.name(),
                                v
                        );
                    })
                    .collect(Collectors.toList());
            var ctorParamsMap = ctorParamsKVP.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            var ctorParamsKeyList = ctorParamsKVP.stream().map(Map.Entry::getKey).collect(Collectors.toList());

            return enumerator -> {
                var params = new Object[ctorParamsKeyList.size()];
                var inverseIndexer = new FastInverseIndexer(params.length);

                ObjectEnumerator.ObjectElement element;
                while(enumerator.hasRemaining() && (element = enumerator.next()) != null) {
                    if(ctorParamsMap.containsKey(element.name)) {
                        var i = ctorParamsKeyList.indexOf(element.name);
                        inverseIndexer.set(i);
                        params[i] = element.value;
                    }
                }
            }
        }
    }

    private class FieldInfo {
        public final String edgedbName;
        public final Class<?> fieldType;
        public final FieldSetter setter;

        private @Nullable Method setMethod;

        public FieldInfo(Field field) {
            this.fieldType = field.getType();

            var nameAnno = field.getAnnotation(EdgeDBName.class);

            if(nameAnno != null) {
                this.edgedbName = nameAnno.name();
            }

            if(this.edgedbName == null) {

            }


            // if there's a set method that isn't ignored, with the same type, use it.
            var setter = setterMethods.stream()
                    .filter(x ->
                            x.getName().equalsIgnoreCase("set" + field.getName()) &&
                            x.getParameterTypes().length == 1 &&
                            x.getParameterTypes()[0].equals(fieldType))
                    .findFirst();

            if(setter.isPresent()) {
                this.setMethod = setter.get();
                this.setter = (i, v) -> {
                    assert this.setMethod != null;
                    this.setMethod.invoke(i, v);
                };
            }
            else {
                this.setter = field::set;
            }
        }
    }

    @FunctionalInterface
    private interface FieldSetter {
        void set(Object instance, Object value) throws ReflectiveOperationException;
    }
}
