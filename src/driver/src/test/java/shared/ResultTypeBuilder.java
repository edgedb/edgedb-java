package shared;

import com.gel.driver.annotations.GelLinkType;
import com.gel.driver.annotations.GelType;
import com.gel.driver.datatypes.Json;
import com.gel.driver.datatypes.Range;
import com.gel.driver.datatypes.RelativeDuration;
import com.gel.driver.datatypes.Tuple;
import com.gel.driver.util.StringsUtil;
import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.classes.*;
import org.jetbrains.annotations.Nullable;
import org.opentest4j.AssertionFailedError;
import shared.models.CollectionResultNode;
import shared.models.ResultNode;
import shared.models.ResultNodeImpl;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ResultTypeBuilder {
    public static Class<?> getClassFromGelTypeName(String name){
        if(StringsUtil.isNullOrEmpty(name)) {
            throw new NullPointerException("Type name was null or empty");
        }

        if(name.contains("::"))
            name = name.split("::")[1];

        switch (name) {
            case "bool":
                return Boolean.class;
            case "bytes":
                return Byte[].class;
            case "str":
                return String.class;
            case "local_date":
                return LocalDate.class;
            case "local_time":
                return LocalTime.class;
            case "local_datetime":
                return LocalDateTime.class;
            case "relative_duration":
                return RelativeDuration.class;
            case "datetime":
                return OffsetDateTime.class;
            case "duration":
                return Duration.class;
            case "date_duration":
                return Period.class;
            case "float32":
                return Float.class;
            case "float64":
                return Double.class;
            case "int16":
                return Short.class;
            case "int32":
                return Integer.class;
            case "int64":
                return Long.class;
            case "decimal":
                return BigDecimal.class;
            case "bigint":
                return BigInteger.class;
            case "json":
                return Json.class;
            case "uuid":
                return UUID.class;
            default:
                throw new RuntimeException("Unmapped type \"" + name + "\"");
        }
    }

    public static @Nullable Class<?> tryGetClassFromGelTypeName(String name) {
        try {
            return getClassFromGelTypeName(name);
        }
        catch(Exception x) {
            return null;
        }
    }

    public static Object toObject(ResultNode node) {
        if(node instanceof CollectionResultNode) {
            var collectionNode = (CollectionResultNode)node;

            var values = (Object[])collectionNode.getValue();

            assert values != null;

            if(values.length == 0) {
                return new Object[0];
            }

            var elementType = tryGetClassFromGelTypeName(collectionNode.getElementType());

            if(elementType == null) {
                elementType = toObject((ResultNode) values[0]).getClass();
            }

            var arr = (Object[])Array.newInstance(elementType, values.length);

            for (int i = 0; i != values.length; i++) {
                arr[i] = toObject((ResultNode) values[i]);
            }

            return arr;
        }
        else if (node instanceof ResultNodeImpl) {
            if(node.getName().equals("tuple")) {
                assert node.getValue() != null;
                return Tuple.of(
                        Arrays.stream(((ResultNode[]) node.getValue()))
                                .map(ResultTypeBuilder::toObject).toArray()
                );
            }

            return node.getValue();
        }

        throw new RuntimeException("Unknown node type " + node.getClass().getName());
    }

    private static class TypeInfo {
        public final Class<?> type;
        public final @Nullable Class<?> elementType;


        private TypeInfo(Class<?> type) {
            this.type = type;
            this.elementType = null;
        }
        private TypeInfo(Class<?> type, @Nullable Class<?> elementType) {
            this.type = type;
            this.elementType = elementType;
        }
    }

    public static List<Class<?>> createResultTypes(Object result, int max, boolean genTypes) {
        var typeCountRef = new AtomicInteger(max);

        if(result instanceof ResultNode[]) {
            return Arrays.stream(((ResultNode[]) result))
                    .flatMap(v -> createResultTypes(v, true, typeCountRef, genTypes).stream())
                    .map(v -> v.type)
                    .collect(Collectors.toList());
        }
        else if (result instanceof ResultNode) {
            return createResultTypes((ResultNode) result, true, typeCountRef, genTypes).stream().map(v -> v.type).collect(Collectors.toList());
        }

        throw new AssertionFailedError("Unknown result object");
    }

    private static List<TypeInfo> createSubResultTypes(ResultNode node, AtomicInteger count, boolean genTypes) {
        return createResultTypes(node, false, count, genTypes);
    }

    private static List<TypeInfo> createResultTypes(ResultNode node, boolean isRoot, AtomicInteger count, boolean genTypes) {
        switch (node.getName()) {
            case "object":
            {
                var result = new ArrayList<TypeInfo>(){{
                    add(new TypeInfo(Map.class));
                }};

                if(genTypes) {
                    result.addAll(createClassDefinition(node, count));
                }

                return result;
            }
            case "tuple":
            case "namedtuple":
            {
                var result = new ArrayList<TypeInfo>() {{
                    add(new TypeInfo(Tuple.class));
                }};

                if(node.getName().equals("namedtuple")) {
                    if(genTypes) {
                        result.addAll(createClassDefinition(node, count));
                    }
                    result.add(new TypeInfo(Map.class));
                }

                return result;
            }
            case "range":
                return new ArrayList<>(){{
                    add(new TypeInfo(Objects.requireNonNull(node.getValue()).getClass(), ((Range<?>)node.getValue()).getElementType()));
                }};
            case "set":
                if(isRoot) {
                    var array = (ResultNode[])node.getValue();

                    assert array != null;

                    var elementTypes = new ArrayList<TypeInfo>(){{
                        add(new TypeInfo(Object.class));
                    }};

                    if(array.length != 0) {
                        elementTypes.addAll(createResultTypes(array[0], false, count, genTypes));
                    }

                    return elementTypes;
                }
            case "array": // set falls through when !isRoot
            {
                var array = (ResultNode[])node.getValue();

                var elementTypes = new ArrayList<TypeInfo>(){{
                    add(new TypeInfo(Object.class));
                }};

                assert array != null;

                if(array.length != 0) {
                    elementTypes.addAll(createResultTypes(array[0], false, count, genTypes));
                }


                var result = new ArrayList<TypeInfo>();

                for(var elementType : elementTypes) {
                    result.add(new TypeInfo(Array.newInstance(elementType.type, 0).getClass()));
                    result.add(new TypeInfo(List.class, elementType.type));
                    result.add(new TypeInfo(ArrayList.class, elementType.type));
                    result.add(new TypeInfo(Collection.class, elementType.type));
                }

                return result;
            }
            case "std::datetime":
                return new ArrayList<>(){{
                    add(new TypeInfo(OffsetDateTime.class));
                    add(new TypeInfo(ZonedDateTime.class));
                }};
            case "cal::relative_duration":
                return new ArrayList<>(){{
                    add(new TypeInfo(RelativeDuration.class));
                    add(new TypeInfo(Period.class));
                    add(new TypeInfo(Duration.class));
                }};
            default:
                if(node.getValue() instanceof ResultNode) {
                    throw new RuntimeException("Unknown node parser for " + ((ResultNode) node.getValue()).getName());
                }

                return new ArrayList<>(){{
                    add(new TypeInfo(node.getValue() == null ? Object.class : node.getValue().getClass()));
                }};
        }
    }

    private static List<TypeInfo> ofIfInBounds(Supplier<TypeInfo> f, AtomicInteger count) {
        if(count.getAndDecrement() > 0) {
            return new ArrayList<>(){{add(f.get());}};
        }

        return new ArrayList<>();
    }

    private static <T> List<TypeInfo> ofIfInBounds(T a, BiFunction<T, AtomicInteger, TypeInfo> f, AtomicInteger count) {
        if(count.getAndDecrement() > 0) {
            return new ArrayList<>(){{add(f.apply(a, count));}};
        }

        return new ArrayList<>();
    }

    private static <T> List<TypeInfo> applyIfInBounds(T a, BiFunction<T, AtomicInteger, List<TypeInfo>> f, AtomicInteger count) {
        if(count.get() > 0) {
            return f.apply(a, count);
        }

        return new ArrayList<>();
    }

    private static void addIfInBounds(List<TypeInfo> list, Supplier<TypeInfo> f, AtomicInteger count) {
        if(count.getAndDecrement() > 0) {
            list.add(f.get());
        }
    }

    private static void addIfInBounds(List<TypeInfo> list, Class<?> cls, AtomicInteger count) {
        addIfInBounds(list, new TypeInfo(cls), count);
    }

    private static void addIfInBounds(List<TypeInfo> list, TypeInfo cls, AtomicInteger count) {
        if(count.getAndDecrement() > 0) {
            list.add(cls);
        }
    }

    private static List<TypeInfo> createClassDefinition(ResultNode node, AtomicInteger count) {
        if(!(node.getValue() instanceof Map)) {
            throw new RuntimeException("Value must be a map");
        }

        var results = new ArrayList<TypeInfo>();

        //noinspection unchecked
        for(var propertyDef : createPropertyDefinitions((Map<String, ResultNode>)node.getValue(), count)) {
            if(propertyDef.size() == 0) {
                return results;
            }

            var typename = getTypeName(node);
            var classDef = ClassSourceGenerator
                    .create(TypeDeclarationSourceGenerator.create(typename))
                    .addAnnotation(AnnotationSourceGenerator.create(GelType.class))
                    .addModifier(Modifier.PUBLIC);

            for(var prop : propertyDef.entrySet()) {
                var field = VariableSourceGenerator.create(prop.getValue().type, prop.getKey()).addModifier(Modifier.PUBLIC);

                if(prop.getValue().elementType != null) {
                    field.addAnnotation(
                            AnnotationSourceGenerator.create(GelLinkType.class)
                                    .addParameter("value", VariableSourceGenerator.create(TypeDeclarationSourceGenerator.create(prop.getValue().elementType), ".class"))
                    );
                }

                classDef.addField(field);
            }

            var sg = UnitSourceGenerator.create("com.gel.runtime").addClass(classDef);

            var componentSupplier = ComponentContainer.getInstance();
            var classFactory = componentSupplier.getClassFactory();


            //noinspection resource
            results.add(new TypeInfo(classFactory.loadOrBuildAndDefine(sg).get("com.gel.runtime." + typename)));

            if(count.decrementAndGet() <= 0) {
                return results;
            }
        }

        return results;
    }

    private static List<Map<String, TypeInfo>> createPropertyDefinitions(Map<String, ResultNode> props, AtomicInteger count) {
        var propMap = new ArrayList<Map.Entry<Map.Entry<String, ResultNode>, List<TypeInfo>>>();
        var result = new ArrayList<Map<String, TypeInfo>>();


        for(var prop : props.entrySet()) {
            var types = createResultTypes(prop.getValue(), false, count, true);

            if(types.isEmpty()) {
                // increment count until types has one type in it
                int t = 0;
                while(types.isEmpty()) {
                    var a = count.getAndIncrement();
                    t++;
                    types = createResultTypes(prop.getValue(), false, count, true);
                    if(count.get() <= a) {
                        count.addAndGet(t);
                    }
                }

                count.set(0);
                var list = new ArrayList<TypeInfo>();
                list.add(types.get(0));
                propMap.add(Map.entry(prop, list));
            } else {
                propMap.add(Map.entry(prop, types));

            }
        }

        for(int i = 0; i != propMap.size(); i++) {
            var prop = propMap.get(i);

            if(IntStream.range(0, i + 1).mapToObj(propMap::get).mapToInt(v -> v.getValue().size()).sum() == i + 1) {
                // there are only one type per property here
                continue;
            }

            for(int j = 0; j != prop.getValue().size(); j++) {
                var type = prop.getValue().get(j);

                var list = new Map.Entry[propMap.size()];

                // populate values outside of 'i'
                for(int k = propMap.size() - 1; k > i; k--) {
                    var entry = propMap.get(k);
                    list[k] = Map.entry(entry.getKey().getKey(), entry.getValue().get(0));
                }

                list[i] = Map.entry(prop.getKey().getKey(), type);

                // populate values in 'i - 1'
                for(int k = i - 1; k >= 0; k--) {
                    var entry = propMap.get(k);
                    list[k] = Map.entry(entry.getKey().getKey(), entry.getValue().get(0));
                }

                // iterate over sub-possibilities
                for(int k = i - 1; k >= 0; k--) {
                    var entry = propMap.get(k);
                    for(int l = 1; l != entry.getValue().size(); l++) {
                        var entryType = entry.getValue().get(l);
                        var copy = Arrays.copyOf(list, list.length);
                        copy[k] = Map.entry(entry.getKey().getKey(), entryType);
                        if(count.getAndDecrement() > 0) {
                            try{
                                result.add(Arrays.stream(copy).collect(Collectors.toMap(v -> (String)v.getKey(), v -> (TypeInfo)v.getValue())));
                            }
                            catch (NullPointerException e) {
                                throw e;
                            }
                        } else  {
                            return result;
                        }
                    }
                }

                if(count.getAndDecrement() > 0) {
                    try {
                        result.add(Arrays.stream(list).collect(Collectors.toMap(v -> (String)v.getKey(), v -> (TypeInfo)v.getValue())));
                    } catch (NullPointerException e) {
                        throw e;
                    }
                } else {
                    // return out
                    return result;
                }
            }
        }

        return result.size() == 0 ? List.of(propMap.stream().collect(Collectors.toMap(v -> v.getKey().getKey(), v -> v.getValue().get(0)))) : result;
    }

    private static final Random STR_RANDOM = new Random();
    private static String getTypeName(ResultNode node) {
        return STR_RANDOM.ints(97, 123)
                .limit(10)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString() + node.hashCode();
    }
}
