package com.edgedb.driver.binary.builders.types;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBIgnore;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.binary.builders.ObjectBuilder;
import com.edgedb.driver.binary.builders.ObjectEnumerator;
import com.edgedb.driver.binary.builders.TypeDeserializerFactory;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.namingstrategies.NamingStrategy;
import com.edgedb.driver.util.FastInverseIndexer;
import com.edgedb.driver.util.TypeUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeDeserializerInfo<T> {
    private static final Logger logger = LoggerFactory.getLogger(TypeDeserializerInfo.class);
    public final TypeDeserializerFactory<T> factory;

    private final Class<T> type;
    private final Method[] methods;

    private final Map<NamingStrategy, NamingStrategyMap<Parameter>> constructorNamingMap;
    private final Map<NamingStrategy, NamingStrategyMap<FieldInfo>> fieldNamingMap;

    public TypeDeserializerInfo(Class<T> type) {
        this.constructorNamingMap = new HashMap<>();
        this.fieldNamingMap = new HashMap<>();
        this.type = type;
        this.methods = type.getDeclaredMethods();

        try {
            this.factory = createFactory();
        } catch (ReflectiveOperationException e) {
            logger.error("Failed to create type deserialization factory", e);
            throw new RuntimeException(e);
        }
    }

    public boolean isValidField(Field field) {
        return field.getAnnotation(EdgeDBIgnore.class) == null;
    }

    @SuppressWarnings("unchecked")
    private TypeDeserializerFactory<T> createFactory() throws ReflectiveOperationException {
        // check for constructor deserializer
        var constructors = this.type.getDeclaredConstructors();

        var ctorDeserializer = Arrays.stream(constructors).filter(x -> x.getAnnotation(EdgeDBDeserializer.class) != null).findFirst();

        if(ctorDeserializer.isPresent()) {
            var ctor = ctorDeserializer.get();

            return enumerator -> {
                var namingStrategyEntry = constructorNamingMap.computeIfAbsent(
                        enumerator.getClient().getConfig().getNamingStrategy(),
                        (n) -> new NamingStrategyMap<>(n, Parameter::getName, ctor.getParameters())
                );

                var params = new Object[namingStrategyEntry.nameIndexMap.size()];
                var inverseIndexer = new FastInverseIndexer(params.length);

                ObjectEnumerator.ObjectElement element;
                while(enumerator.hasRemaining() && (element = enumerator.next()) != null) {
                    if(namingStrategyEntry.map.containsKey(element.name)) {
                        var i = namingStrategyEntry.nameIndexMap.get(element.name);
                        inverseIndexer.set(i);
                        params[i] = element.value;
                    }
                }

                var missed = inverseIndexer.getInverseIndexes();

                for(int i = 0; i != missed.length; i++) {
                    params[missed[i]] = TypeUtils.getDefaultValue(namingStrategyEntry.values[i].getType());
                }

                return (T)ctor.newInstance(params);
            };
        }

        // abstract or interface: TODO
        if(type.isInterface() || Modifier.isAbstract(type.getModifiers())) {

        }

        // default case
        var emptyCtor = Arrays.stream(constructors).filter(v -> v.getParameterCount() == 0).findFirst();

        if(emptyCtor.isEmpty()) {
            throw new ReflectiveOperationException(String.format("No empty constructor found to construct the type %s", this.type));
        }

        var ctor = emptyCtor.get();

        var setterMethods = Arrays.stream(methods)
                .filter((v) -> v.getName().startsWith("set"))
                .collect(Collectors.toMap(v -> v.getName().toLowerCase(), v -> v));

        var fields = type.getDeclaredFields();

        var validFields = new FieldInfo[fields.length];

        for(int i = 0; i != validFields.length; i++) {
            var field = fields[i];
            if(isValidField(field)) {
                validFields[i] = new FieldInfo(field, setterMethods);
            }
        }

        // default case
        return enumerator -> {
            var namingStrategyEntry = fieldNamingMap.computeIfAbsent(
                    enumerator.getClient().getConfig().getNamingStrategy(),
                    (v) -> new NamingStrategyMap<>(v, FieldInfo::getFieldName, validFields)
            );

            var instance = ctor.newInstance();
            ObjectEnumerator.ObjectElement element;

            while (enumerator.hasRemaining() && (element = enumerator.next()) != null) {
                if(namingStrategyEntry.map.containsKey(element.name)) {
                    var fieldInfo = namingStrategyEntry.map.get(element.name);
                    fieldInfo.convertAndSet(enumerator.getClient().getConfig().UseFieldSetters(), instance, element.value);
                }
            }

            return (T)instance;
        };
    }

    private static class FieldInfo {
        public final EdgeDBName edgedbNameAnno;
        public final Class<?> fieldType;
        public final Field field;
        private final @Nullable Method setMethod;

        public FieldInfo(Field field, Map<String, Method> setters) {
            this.field = field;
            this.fieldType = field.getType();

            this.edgedbNameAnno = field.getAnnotation(EdgeDBName.class);

            // if there's a set method that isn't ignored, with the same type, use it.
            this.setMethod = setters.get("set" + field.getName());
        }

        public String getFieldName() {
            return this.field.getName();
        }

        public void convertAndSet(boolean useMethodSetter, Object instance, Object value) throws EdgeDBException, ReflectiveOperationException {
            var converted = convertToType(value);

            if(useMethodSetter && setMethod != null) {
                setMethod.invoke(instance, converted);
            } else {
                field.set(instance, converted);
            }
        }

        private Object convertToType(Object value) throws EdgeDBException {
            // TODO: custom converters?

            if(value == null) {
                return TypeUtils.getDefaultValue(fieldType);
            }

            return ObjectBuilder.convertTo(fieldType, value);
        }
    }

    private static class NamingStrategyMap<T> {
        public final NamingStrategy strategy;
        public final Map<String, T> map;
        public final Map<String, Integer> nameIndexMap;
        public final T[] values;

        public NamingStrategyMap(NamingStrategy strategy, Function<T, String> getName, T[] values) {
            this.map = new HashMap<>(values.length);
            this.nameIndexMap = new HashMap<>(values.length);
            this.values = values;
            this.strategy = strategy;

            for (int i = 0; i < values.length; i++) {
                var value = values[i];

                if(value == null)
                    continue;

                var name = strategy.convert(getName.apply(value));
                map.put(name, value);
                nameIndexMap.put(name, i);
            }
        }
    }
}
