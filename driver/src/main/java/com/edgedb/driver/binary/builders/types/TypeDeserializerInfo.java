package com.edgedb.driver.binary.builders.types;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBIgnore;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.binary.builders.ObjectBuilder;
import com.edgedb.driver.binary.builders.ObjectEnumerator;
import com.edgedb.driver.binary.builders.TypeDeserializerFactory;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.namingstrategies.NamingStrategy;
import com.edgedb.driver.util.FastInverseIndexer;
import com.edgedb.driver.util.TypeUtils;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeDeserializerInfo<T> {
    private static final Logger logger = LoggerFactory.getLogger(TypeDeserializerInfo.class);
    public final TypeDeserializerFactory<T> factory;
    private final Reflections reflection;
    private final Class<T> type;
    private final Method[] methods;

    private final Map<NamingStrategy, NamingStrategyMap<Parameter>> constructorNamingMap;
    private final Map<NamingStrategy, NamingStrategyMap<FieldInfo>> fieldNamingMap;
    private final @Nullable EdgeDBType edgeDBTypeAnno;

    private final Map<String, TypeDeserializerInfo<? extends T>> children;

    public TypeDeserializerInfo(Class<T> type) {
        this.constructorNamingMap = new HashMap<>();
        this.fieldNamingMap = new HashMap<>();
        this.type = type;
        this.methods = type.getDeclaredMethods();
        this.reflection = new Reflections(type);
        this.edgeDBTypeAnno = type.getAnnotation(EdgeDBType.class);

        // find potential children
        var children = this.reflection.getSubTypesOf(type);

        if(!children.isEmpty()) {
            this.children = new HashMap<>(children.size());
            for (var child : children) {
                if(child.getAnnotation(EdgeDBIgnore.class) != null) {
                    continue;
                }

                var typeInfo = TypeBuilder.getDeserializerInfo(child);

                if(typeInfo == null) {
                    continue;
                }

                this.children.put(typeInfo.type.getSimpleName(), typeInfo);
            }
        } else {
            this.children = Map.of();
        }

        try {
            this.factory = createFactory();
        } catch (ReflectiveOperationException | EdgeDBException e) {
            logger.error("Failed to create type deserialization factory", e);
            throw new RuntimeException(e);
        }
    }

    public boolean requiresTypeNameIntrospection() {
        return !this.children.isEmpty();
    }

    public @Nullable String getModuleName() {
        return this.edgeDBTypeAnno == null ? null : this.edgeDBTypeAnno.module();
    }

    private boolean isValidField(Field field) {
        return field.getAnnotation(EdgeDBIgnore.class) == null;
    }

    @SuppressWarnings("unchecked")
    private TypeDeserializerFactory<T> createFactory() throws ReflectiveOperationException, EdgeDBException {
        // check for constructor deserializer
        var constructors = this.type.getDeclaredConstructors();

        var ctorDeserializer = Arrays.stream(constructors).filter(x -> x.getAnnotation(EdgeDBDeserializer.class) != null).findFirst();

        if(ctorDeserializer.isPresent()) {
            var ctor = ctorDeserializer.get();

            return (enumerator, parent) -> {
                var namingStrategyEntry = constructorNamingMap.computeIfAbsent(
                        enumerator.getClient().getConfig().getNamingStrategy(),
                        (n) -> new NamingStrategyMap<>(n, (v) -> getNameOrAnnotated(v, Parameter::getName), ctor.getParameters())
                );

                var params = new Object[namingStrategyEntry.nameIndexMap.size()];
                var inverseIndexer = new FastInverseIndexer(params.length);

                ObjectEnumerator.ObjectElement element;

                var unhandled = new Vector<ObjectEnumerator.ObjectElement>(params.length);

                while(enumerator.hasRemaining() && (element = enumerator.next()) != null) {
                    if(namingStrategyEntry.map.containsKey(element.name)) {
                        var i = namingStrategyEntry.nameIndexMap.get(element.name);
                        inverseIndexer.set(i);
                        params[i] = element.value;
                    } else {
                        unhandled.add(element);
                    }
                }

                var missed = inverseIndexer.getInverseIndexes();

                for(int i = 0; i != missed.length; i++) {
                    params[missed[i]] = TypeUtils.getDefaultValue(namingStrategyEntry.values[i].getType());
                }

                var instance = (T)ctor.newInstance(params);

                if(parent != null) {
                    for (var unhandledElement : unhandled) {
                        parent.accept(instance, unhandledElement);
                    }
                }

                return instance;
            };
        }

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

        // abstract or interface
        if(type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            if(this.children.isEmpty()) {
                throw new EdgeDBException(
                        "Cannot deserialize to type " + this.type + "; no sub-types found to " +
                        "deserialize to for this interface/abstract class"
                );
            }

            return (enumerator, parent) -> {
                var namingStrategyEntry = fieldNamingMap.computeIfAbsent(
                        enumerator.getClient().getConfig().getNamingStrategy(),
                        (v) -> new NamingStrategyMap<>(v, (u) -> getNameOrAnnotated(u.field, Field::getName), validFields)
                );

                var element = enumerator.next();

                if(element == null) {
                    throw new EdgeDBException("No data left in object enumerator for type building");
                }

                //noinspection SpellCheckingInspection
                if(!element.name.equals("__tname__")) {
                    throw new EdgeDBException("Type introspection is required for deserializing abstract classes or interfaces");
                }

                var split = ((String)element.value).split("::");

                for (var child : children.entrySet()) {
                    var module = child.getValue().getModuleName();

                    if((module == null || split[0] == module) && child.getKey().equals(split[1])) {
                        return child.getValue().factory.deserialize(enumerator, (i, v) -> {
                            if(namingStrategyEntry.map.containsKey(v.name)) {
                                var fieldInfo = namingStrategyEntry.map.get(v.name);
                                fieldInfo.convertAndSet(enumerator.getClient().getConfig().useFieldSetters(), i, v.value);
                            } else if(parent != null) {
                                parent.accept(i, v);
                            }
                        });
                    }
                }

                throw new EdgeDBException(String.format("No child found for abstract type %s matching the name \"%s::%s\"", this.type.getName(), split[0], split[1]));
            };
        }

        // default case
        var emptyCtor = Arrays.stream(constructors).filter(v -> v.getParameterCount() == 0).findFirst();

        if(emptyCtor.isEmpty()) {
            throw new ReflectiveOperationException(String.format("No empty constructor found to construct the type %s", this.type));
        }

        var ctor = emptyCtor.get();

        // default case
        return (enumerator, parent) -> {
            var namingStrategyEntry = fieldNamingMap.computeIfAbsent(
                    enumerator.getClient().getConfig().getNamingStrategy(),
                    (v) -> new NamingStrategyMap<>(v, (u) -> getNameOrAnnotated(u.field, Field::getName), validFields)
            );

            var instance = (T)ctor.newInstance();
            ObjectEnumerator.ObjectElement element;

            while (enumerator.hasRemaining() && (element = enumerator.next()) != null) {
                if(namingStrategyEntry.map.containsKey(element.name)) {
                    var fieldInfo = namingStrategyEntry.map.get(element.name);
                    fieldInfo.convertAndSet(enumerator.getClient().getConfig().useFieldSetters(), instance, element.value);
                } else if(parent != null) {
                    parent.accept(instance, element);
                }
            }

            return instance;
        };
    }

    private <U extends AnnotatedElement> String getNameOrAnnotated(U value, Function<U, String> getName) {
        var anno = value.getAnnotation(EdgeDBName.class);
        if(anno != null && anno.value() != null) {
            return anno.value();
        }

        return getName.apply(value);
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
