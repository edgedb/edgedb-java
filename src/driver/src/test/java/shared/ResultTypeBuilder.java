package shared;

import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.datatypes.Json;
import com.edgedb.driver.datatypes.RelativeDuration;
import com.edgedb.driver.datatypes.Tuple;
import com.edgedb.driver.util.StringsUtil;
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
import java.util.stream.Collectors;

public class ResultTypeBuilder {
    public static Class<?> getClassFromEdgeDBTypeName(String name){
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

    public static @Nullable Class<?> tryGetClassFromEdgeDBTypeName(String name) {
        try {
            return getClassFromEdgeDBTypeName(name);
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

            var elementType = tryGetClassFromEdgeDBTypeName(collectionNode.getElementType());

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
                return new Tuple(
                        Arrays.stream(((ResultNode[]) node.getValue()))
                        .map(ResultTypeBuilder::toObject).toArray()
                );
            }

            return node.getValue();
        }

        throw new RuntimeException("Unknown node type " + node.getClass().getName());
    }

    public static List<Class<?>> createResultTypes(Object result) {
        if(result instanceof ResultNode[]) {
            return Arrays.stream(((ResultNode[]) result))
                    .flatMap(v -> createResultTypes(v).stream())
                    .collect(Collectors.toList());
        }
        else if (result instanceof ResultNode) {
            return createResultTypes((ResultNode) result);
        }

        throw new AssertionFailedError("Unknown result object");
    }

    public static List<Class<?>> createResultTypes(ResultNode node) {
        return createResultTypes(node, true);
    }

    private static List<Class<?>> createResultTypes(ResultNode node, boolean isRoot) {
        switch (node.getName()) {
            case "object":
            {
                var result = createClassDefinition(node);
                result.add(Map.class);
            }
            case "tuple":
            case "namedtuple":
            {
                var result = new ArrayList<Class<?>>() {{
                    add(Tuple.class);
                }};

                if(node.getName().equals("namedtuple")) {
                    result.addAll(createClassDefinition(node));
                    result.add(Map.class);
                }
                return  result;
            }
            case "range":
                return new ArrayList<>(){{
                    add(Objects.requireNonNull(node.getValue()).getClass());
                }};
            case "set":
                if(isRoot) {
                    var array = (ResultNode[])node.getValue();

                    assert array != null;

                    var elementTypes = new ArrayList<Class<?>>() {{
                        add(Object.class);
                    }};

                    if(array.length != 0) {
                        elementTypes.addAll(createResultTypes(array[0], false));
                    }

                    return elementTypes;
                }
            case "array": // set falls through when !isRoot
            {
                var array = (ResultNode[])node.getValue();

                var elementTypes = new ArrayList<Class<?>>() {{
                    add(Object.class);
                }};

                assert array != null;

                if(array.length != 0) {
                    elementTypes.addAll(createResultTypes(array[0], false));
                }

                var result = new ArrayList<Class<?>>() {{
                    add(List.class);
                    add(ArrayList.class);
                    add(Collection.class);
                }};

                for(var elementType : elementTypes) {
                    result.add(Array.newInstance(elementType, 0).getClass());
                }

                return result;
            }
            case "std::datetime":
                return new ArrayList<>(){{
                    add(OffsetDateTime.class);
                    add(ZonedDateTime.class);
                }};
            case "cal::relative_duration":
                return new ArrayList<>(){{
                    add(RelativeDuration.class);
                    add(Period.class);
                    add(Duration.class);
                }};
            default:
                if(node.getValue() instanceof ResultNode) {
                    throw new RuntimeException("Unknown node parser for " + ((ResultNode) node.getValue()).getName());
                }

                return new ArrayList<>(){{
                    add(node.getValue() == null ? Object.class : node.getValue().getClass());
                }};
        }
    }

    private static final UnitSourceGenerator SOURCE_GENERATOR = UnitSourceGenerator.create("com.edgedb.runtime");
    private static List<Class<?>> createClassDefinition(ResultNode node) {
        if(!(node.getValue() instanceof Map)) {
            throw new RuntimeException("Value must be a map");
        }

        var results = new ArrayList<Class<?>>();

        //noinspection unchecked
        for(var propertyDef : createPropertyDefinitions((Map<String, ResultNode>)node.getValue())) {
            var typename = getTypeName(node);
            var classDef = ClassSourceGenerator
                    .create(TypeDeclarationSourceGenerator.create(typename))
                    .addAnnotation(AnnotationSourceGenerator.create(EdgeDBType.class))
                    .addModifier(Modifier.PUBLIC);

            for(var prop : propertyDef.entrySet()) {
                classDef.addField(VariableSourceGenerator.create(prop.getValue(), prop.getKey()).addModifier(Modifier.PUBLIC));
            }

            SOURCE_GENERATOR.addClass(classDef);

            var componentSupplier = ComponentContainer.getInstance();
            var classFactory = componentSupplier.getClassFactory();


            //noinspection resource
            results.add(classFactory.loadOrBuildAndDefine(SOURCE_GENERATOR).get("com.edgedb.runtime." + typename));
        }

        return results;
    }

    private static List<Map<String, Class<?>>> createPropertyDefinitions(Map<String, ResultNode> props) {
        var maps = new ArrayList<Map<String,Class<?>>>();

        for(var prop : props.entrySet()) {
            var types = createResultTypes(prop.getValue());
            var dict = new ArrayList<Map<String,Class<?>>>();

            if(!maps.isEmpty()) {
                for(var map : maps) {
                    for(var type : types) {
                        dict.add(new HashMap<>(map) {{
                            put(prop.getKey(), type);
                        }});
                    }
                }
            } else {
                for(var type : types) {
                    dict.add(new HashMap<>() {{
                        put(prop.getKey(), type);
                    }});
                }
            }

            maps = dict;
        }

        return maps;
    }

    private static final Random STR_RANDOM = new Random();
    private static String getTypeName(ResultNode node) {
        return STR_RANDOM.ints(97, 123)
                .limit(10)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString() + node.hashCode();
    }
}
