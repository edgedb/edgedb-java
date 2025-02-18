package com.edgedb.driver.binary.builders.types;

import com.edgedb.driver.ObjectEnumerator;
import com.edgedb.driver.annotations.*;
import com.edgedb.driver.binary.builders.ObjectBuilder;
import com.edgedb.driver.binary.builders.TypeDeserializerFactory;
import com.edgedb.driver.binary.builders.internal.ObjectEnumeratorImpl;
import com.edgedb.driver.binary.protocol.common.Cardinality;
import com.edgedb.driver.exceptions.GelException;
import com.edgedb.driver.exceptions.NoTypeConverterException;
import com.edgedb.driver.namingstrategies.NamingStrategy;
import com.edgedb.driver.util.FastInverseIndexer;
import com.edgedb.driver.util.StringsUtil;
import com.edgedb.driver.util.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TypeDeserializerInfo<T> {
    private static final Logger logger = LoggerFactory.getLogger(TypeDeserializerInfo.class);
    public final TypeDeserializerFactory<T> factory;

    private final Class<T> type;
    private final @Nullable GelType edgeDBTypeAnno;

    // lazy fields, use getter methods
    private List<FieldInfo> fields;
    private Map<String, Method> setterMethods;
    private Reflections reflection;
    private Collection<Class<?>> bases;

    private final @NotNull Map<NamingStrategy, NamingStrategyMap<Parameter>> constructorNamingMap;
    private final @NotNull Map<NamingStrategy, NamingStrategyMap<FieldInfo>> fieldNamingMap;
    private final @NotNull Map<String, TypeDeserializerInfo<? extends T>> children;

    public TypeDeserializerInfo(@NotNull Class<T> type) {
        this.constructorNamingMap = new HashMap<>();
        this.fieldNamingMap = new HashMap<>();
        this.type = type;
        this.edgeDBTypeAnno = type.getAnnotation(GelType.class);
        this.children = new HashMap<>();

        try {
            this.factory = createFactory();
        } catch (ReflectiveOperationException e) {
            logger.error("Failed to create type deserialization factory", e);
            throw new RuntimeException(e);
        }
    }

    public TypeDeserializerInfo(Class<T> cls, TypeDeserializerFactory<T> factory) {
        this.type = cls;
        this.factory = factory;
        this.edgeDBTypeAnno = type.getAnnotation(GelType.class);
        this.constructorNamingMap = new HashMap<>();
        this.fieldNamingMap = new HashMap<>();
        this.children = new HashMap<>();
    }

    private synchronized @NotNull Reflections getReflection() {
        if(reflection == null) {
            this.reflection = new Reflections(this.type);
        }

        return this.reflection;
    }

    private synchronized @NotNull Collection<Class<?>> getBases() {
        if(bases == null) {
            var bases = new ArrayList<Class<?>>();
            Class<?> tempType = type;
            Class<?> base;
            while((base = tempType.getSuperclass()) != null && base.isAnnotationPresent(GelType.class)) {
                bases.add(base);
                tempType = base;
            }

            this.bases = bases;
        }

        return bases;
    }

    private synchronized @NotNull List<FieldInfo> getFields() {
        if(fields == null) {
            var fields = type.getDeclaredFields();
            var setterMethods = getSetterMethods();
            var validFields = new ArrayList<FieldInfo>(fields.length);

            for(int i = 0; i != fields.length; i++) {
                var field = fields[i];
                if(isValidField(field)) {
                    validFields.add(new FieldInfo(field, setterMethods));
                }
            }

            this.fields = validFields;
        }
        return fields;
    }

    public synchronized @NotNull Map<String, Method> getSetterMethods() {
        if(setterMethods == null) {
            var methods = type.getDeclaredMethods();
            var setterMethods = Arrays.stream(methods);

            for(var base : getBases()) {
                setterMethods = Stream.concat(setterMethods, Arrays.stream(base.getDeclaredMethods()));
            }

            // kotlin & java use the 'set' prefix, scala uses the '_$eq' postfix.
            this.setterMethods = setterMethods
                    .filter((v) -> v.getName().startsWith("set") || v.getName().endsWith("_$eq"))
                    .collect(Collectors.toMap(v -> {
                        if(v.getName().startsWith("set")) {
                            return v.getName().substring(3);
                        } else {
                            return v.getName().substring(0, v.getName().length() - 4);
                        }
                    }, v -> v));
        }

        return setterMethods;
    }

    public Class<T> getType() {
        return type;
    }

    public void scanChildren() {
        // find potential children
        var children = getReflection().getSubTypesOf(type);

        if(!children.isEmpty()) {
            for (var child : children) {
                if(child.getAnnotation(GelIgnore.class) != null) {
                    continue;
                }

                var typeInfo = TypeBuilder.getDeserializerInfo(child);

                if(typeInfo == null) {
                    continue;
                }

                this.children.putIfAbsent(typeInfo.type.getSimpleName(), typeInfo);
            }
        }
    }

    public boolean requiresTypeNameIntrospection() {
        return !this.children.isEmpty();
    }

    public @Nullable String getModuleName() {
        if(this.edgeDBTypeAnno == null) {
            return null;
        }

        var module = this.edgeDBTypeAnno.module();

        if(StringsUtil.isNullOrEmpty(module) || module.equals("[UNASSIGNED]")) {
            return null;
        }

        return module;
    }

    private boolean isValidField(@NotNull Field field) {
        return field.getAnnotation(GelIgnore.class) == null;
    }

    @SuppressWarnings("unchecked")
    private @NotNull TypeDeserializerFactory<T> createFactory() throws ReflectiveOperationException {
        // check for constructor deserializer
        var constructors = this.type.getDeclaredConstructors();

        var ctorDeserializer = Arrays.stream(constructors).filter(x -> x.getAnnotation(GelDeserializer.class) != null).findFirst();

        if(ctorDeserializer.isPresent()) {
            var ctor = ctorDeserializer.get();
            var ctorParams = ctor.getParameters();
            if(ctorParams.length == 1 && ctorParams[0].getType().equals(ObjectEnumerator.class)) {
                return (enumerator, parent) -> (T)ctor.newInstance(enumerator);
            }

            return (enumerator, parent) -> {
                var namingStrategyEntry = constructorNamingMap.computeIfAbsent(
                        ((ObjectEnumeratorImpl)enumerator).getClient().getConfig().getNamingStrategy(),
                        (n) -> new NamingStrategyMap<>(n, (v) -> getNameOrAnnotated(v, Parameter::getName), ctor.getParameters())
                );

                var params = new Object[namingStrategyEntry.nameIndexMap.size()];
                var inverseIndexer = new FastInverseIndexer(params.length);

                ObjectEnumerator.ObjectElement element;

                var unhandled = new Vector<ObjectEnumerator.ObjectElement>(params.length);

                while(enumerator.hasRemaining() && (element = enumerator.next()) != null) {
                    if(namingStrategyEntry.map.containsKey(element.getName())) {
                        var i = namingStrategyEntry.nameIndexMap.get(element.getName());
                        inverseIndexer.set(i);
                        params[i] = element.getValue();
                    } else {
                        unhandled.add(element);
                    }
                }

                var missed = inverseIndexer.getInverseIndexes();

                for(int i = 0; i != missed.length; i++) {
                    params[missed[i]] = TypeUtils.getDefaultValue(namingStrategyEntry.values.get(i).getType());
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

        // abstract or interface
        if(type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return (enumerator, parent) -> {
                if(this.children.isEmpty()) {
                    throw new GelException(
                            "Cannot deserialize to type " + this.type + "; no sub-types found to " +
                            "deserialize to for this interface/abstract class"
                    );
                }

                var namingStrategyEntry = fieldNamingMap.computeIfAbsent(
                        ((ObjectEnumeratorImpl)enumerator).getClient().getConfig().getNamingStrategy(),
                        (v) -> new NamingStrategyMap<>(v, (u) -> getNameOrAnnotated(u.field, Field::getName), getFields())
                );

                var element = enumerator.next();

                if(element == null) {
                    throw new GelException("No data left in object enumerator for type building");
                }

                //noinspection SpellCheckingInspection
                if(!element.getName().equals("__tname__")) {
                    throw new GelException("Type introspection is required for deserializing abstract classes or interfaces");
                }

                var split = ((String)element.getValue()).split("::");

                for (var child : children.entrySet()) {
                    var module = child.getValue().getModuleName();

                    if((module == null || split[0].equals(module)) && child.getKey().equals(split[1])) {
                        return child.getValue().factory.deserialize(enumerator, (i, v) -> {
                            if(namingStrategyEntry.map.containsKey(v.getName())) {
                                var fieldInfo = namingStrategyEntry.map.get(v.getName());
                                fieldInfo.convertAndSet(((ObjectEnumeratorImpl)enumerator).getClient().getConfig().useFieldSetters(), i, v.getValue());
                            } else if(parent != null) {
                                parent.accept(i, v);
                            }
                        });
                    }
                }

                throw new GelException(String.format("No child found for abstract type %s matching the name \"%s::%s\"", this.type.getName(), split[0], split[1]));
            };
        }

        // default case
        var emptyCtor = Arrays.stream(constructors).filter(v -> v.getParameterCount() == 0).findFirst();

        if(emptyCtor.isEmpty()) {
            throw new ReflectiveOperationException(String.format("No empty constructor found to construct the type %s", this.type));
        }

        var ctor = emptyCtor.get();

        return (enumerator, parent) -> {
            var namingStrategyEntry = fieldNamingMap.computeIfAbsent(
                    ((ObjectEnumeratorImpl)enumerator).getClient().getConfig().getNamingStrategy(),
                    (v) -> new NamingStrategyMap<>(v, (u) -> getNameOrAnnotated(u.field, Field::getName), getFields())
            );

            var instance = (T)ctor.newInstance();
            ObjectEnumerator.ObjectElement element;

            while (enumerator.hasRemaining() && (element = enumerator.next()) != null) {
                if(namingStrategyEntry.map.containsKey(element.getName())) {
                    var fieldInfo = namingStrategyEntry.map.get(element.getName());
                    fieldInfo.convertAndSet(((ObjectEnumeratorImpl)enumerator).getClient().getConfig().useFieldSetters(), instance, element.getValue());
                } else if(parent != null) {
                    parent.accept(instance, element);
                }
            }

            return instance;
        };
    }

    public @NotNull NamingStrategyMap<FieldInfo> getFieldMap(NamingStrategy strategy) {
        return fieldNamingMap.computeIfAbsent(
                strategy,
                (v) -> new NamingStrategyMap<>(v, (u) -> getNameOrAnnotated(u.field, Field::getName), getFields())
        );
    }

    private <U extends AnnotatedElement> String getNameOrAnnotated(@NotNull U value, @NotNull Function<U, String> getName) {
        var anno = value.getAnnotation(EdgeDBName.class);
        if(anno != null && anno.value() != null) {
            return anno.value();
        }

        return getName.apply(value);
    }

    public static class FieldInfo {
        public final EdgeDBName edgedbNameAnno;
        public final @NotNull Class<?> fieldType;
        public final @NotNull Field field;
        private final @Nullable Method setMethod;

        private final @Nullable EdgeDBLinkType linkType;

        public FieldInfo(@NotNull Field field, @NotNull Map<String, Method> setters) {
            this.field = field;
            this.fieldType = field.getType();

            this.edgedbNameAnno = field.getAnnotation(EdgeDBName.class);

            // if there's a set method that isn't ignored, with the same type, use it.
            var setMethod = setters.get(field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1));
            if(setMethod == null) {
                setMethod = setters.get(field.getName());
            }

            this.setMethod = setMethod;

            this.linkType = field.getAnnotation(EdgeDBLinkType.class);
        }

        public Class<?> getType(@Nullable Cardinality cardinality) throws GelException {
            var isCollectionLike = cardinality == Cardinality.MANY ||  fieldType.isArray() || Collection.class.isAssignableFrom(fieldType);

            if(isCollectionLike) {
                return extractCollectionInnerType(fieldType);
            }

            return fieldType;
        }

        private Class<?> extractCollectionInnerType(@NotNull Class<?> cls) throws GelException {
            if(linkType != null) {
                return linkType.value();
            }

            if (cls.isArray()) {
                return fieldType.getComponentType();
            }

            if (Collection.class.isAssignableFrom(cls)) {
                var generic = field.getGenericType();

                if (!(generic instanceof ParameterizedType)) {
                    throw new GelException("Unable to resolve generic parameter in collection for " + field);
                }

                var actualGenerics = ((ParameterizedType) generic).getActualTypeArguments();

                if (actualGenerics.length != 1) {
                    throw new GelException(
                            "Unable to resolve generic parameter for " + field + ", expected 1 generic " +
                                    "type argument, but found " + actualGenerics.length
                    );
                }

                return (Class<?>) actualGenerics[0];
            }

            throw new GelException("Cannot find element type of the collection " + cls.getName());
        }

        public @NotNull String getFieldName() {
            return this.field.getName();
        }

        public void convertAndSet(boolean useMethodSetter, Object instance, Object value) throws GelException, ReflectiveOperationException {
            Object converted;

            try {
                converted = convertToType(value);
            } catch (GelException error) {
                var valueType = value == null ? "NULL" : value.getClass().getName();
                throw new NoTypeConverterException(
                        String.format("The field '%s' with type '%s' cannot be implicitly assigned to the received data type '%s'", field.getName(), field.getType().getName(), valueType)
                );
            }

            if(useMethodSetter && setMethod != null) {
                setMethod.invoke(instance, converted);
            } else {
                field.set(instance, converted);
            }
        }

        private @Nullable Object convertToType(@Nullable Object value) throws GelException {
            // TODO: custom converters?

            if(value == null) {
                return TypeUtils.getDefaultValue(fieldType);
            }

            return ObjectBuilder.convertTo(fieldType, value);
        }
    }

    public static class NamingStrategyMap<T> {
        public final @NotNull NamingStrategy strategy;
        public final @NotNull Map<String, T> map;
        public final @NotNull Map<String, Integer> nameIndexMap;
        public final @NotNull List<T> values;

        public NamingStrategyMap(@NotNull NamingStrategy strategy, @NotNull Function<T, String> getName, @NotNull List<T> values) {
            this.map = new HashMap<>(values.size());
            this.nameIndexMap = new HashMap<>(values.size());
            this.values = values;
            this.strategy = strategy;

            for (int i = 0; i < values.size(); i++) {
                var value = values.get(i);

                if(value == null)
                    continue;

                var name = strategy.convert(getName.apply(value));
                map.put(name, value);
                nameIndexMap.put(name, i);
            }
        }

        public NamingStrategyMap(@NotNull NamingStrategy strategy, @NotNull Function<T, String> getName, T[] values) {
            this(strategy, getName, List.of(values));
        }

        public boolean contains(String name) {
            return this.map.containsKey(name);
        }

        public @Nullable T get(String name) {
            return this.map.get(name);
        }

        public @Nullable T get(int index) {
            return this.values.get(index);
        }
    }
}
