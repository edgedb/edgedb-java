package shared;

import com.gel.driver.datatypes.Json;
import com.gel.driver.datatypes.Range;
import com.gel.driver.datatypes.RelativeDuration;
import com.gel.driver.datatypes.Tuple;
import org.assertj.core.util.Lists;
import org.opentest4j.AssertionFailedError;
import shared.models.ResultNode;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ResultAsserter {

    @FunctionalInterface
    private interface Reducer<T, U> {
        U reduce(T value);
    }

    public static class ReducerEntry<T, U> {
        public final Class<U> fromCls;
        public final Reducer<U, T> reducer;

        public ReducerEntry(Class<U> cls, Reducer<U, T> reducer){
            this.fromCls = cls;
            this.reducer = reducer;
        }

        @SuppressWarnings("unchecked")
        public T reduce(Object value) {
            return reducer.reduce((U)value);
        }
    }

    private static class ReducerContainer<T> {
        private final Map<Class<?>, ReducerEntry<T, ?>> reducerMap;

        public ReducerContainer(List<ReducerEntry<T, ?>> reducers) {
            reducerMap = reducers.stream()
                    .collect(Collectors.toMap(v -> v.fromCls, v -> v));
        }

        @SafeVarargs
        public ReducerContainer(ReducerEntry<T, ?>... reducers) {
            this(List.of(reducers));
        }

        public boolean canReduce(Class<?> a, Class<?> b) {
            return reducerMap.containsKey(a) && reducerMap.containsKey(b);
        }

        public Map.Entry<T, T> reduce(Object a, Object b) {
            return Map.entry(
                    reducerMap.get(a.getClass()).reduce(a),
                    reducerMap.get(b.getClass()).reduce(b)
            );
        }
    }

    private static final List<ReducerContainer<?>> reducers;

    static {
        reducers = new ArrayList<>(){{
            // Json -> string
            add(new ReducerContainer<>(
                    new ReducerEntry<>(Json.class, Json::getValue)
            ));

            // datetime
            add(new ReducerContainer<>(
                    new ReducerEntry<>(ZonedDateTime.class, v -> v.getLong(ChronoField.INSTANT_SECONDS)),
                    new ReducerEntry<>(OffsetDateTime.class, v -> v.getLong(ChronoField.INSTANT_SECONDS))
            ));

            add(new ReducerContainer<>(
                    new ReducerEntry<>(RelativeDuration.class, v -> (long)(v.getMicroseconds() + (v.getDays() + v.getMonths() * 31L) * 8.64e+10)),
                    new ReducerEntry<>(Period.class, v -> (long)((v.toTotalMonths() * 31 + v.getDays()) * 8.64e+10)),
                    new ReducerEntry<>(Duration.class, TimeUnit.MICROSECONDS::convert)
            ));

            // json does this automatically, added here to fix assets from json->edgedb
            add(new ReducerContainer<>(
                    new ReducerEntry<>(BigDecimal.class, BigDecimal::stripTrailingZeros)
            ));
        }};
    }

    public static void assertResult(Object expected, Object actual) {
        if(expected instanceof ResultNode[]) {
            var collection = assertCollection(expected, actual, true);

            for(var kvp : collection.entrySet()) {
                assertNode(kvp.getKey(), kvp.getValue());
            }
        } else if (expected instanceof ResultNode) {
            assertNode((ResultNode) expected, actual);
        } else {
            throw new AssertionFailedError("Unknown expected type " + expected.getClass().getName());
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertNode(ResultNode node, Object actual) {
        switch (node.getName()) {
            case "object":
            {
                assert node.getValue() != null;
                assert actual != null;

                assert node.getValue() instanceof Map;

                var expectedMap = (Map<String, ResultNode>)node.getValue();
                var actualMap = extractMap(actual);

                assert expectedMap.size() == actualMap.size();

                for(var expectedKVP : expectedMap.entrySet()) {
                    assert actualMap.containsKey(expectedKVP.getKey());

                    assertNode(expectedKVP.getValue(), actualMap.get(expectedKVP.getKey()));
                }
            }
            break;
            case "namedtuple":
            {
                assert node.getValue() != null;
                assert actual != null;

                assert node.getValue() instanceof Map;
                var expectedMap = ((Map<String, ResultNode>)node.getValue());

                if(actual instanceof Tuple) {
                    var actualTuple = (Tuple)actual;
                    var expectedValues = expectedMap.values().toArray(new ResultNode[0]);

                    for(int i = 0; i != expectedValues.length; i++){
                        assertNode(expectedValues[i], actualTuple.get(i));
                    }
                } else {
                    var actualMap = extractMap(actual);

                    assert expectedMap.size() == actualMap.size();

                    for(var expectedKVP : expectedMap.entrySet()) {
                        assert actualMap.containsKey(expectedKVP.getKey());

                        assertNode(expectedKVP.getValue(), actualMap.get(expectedKVP.getKey()));
                    }
                }
            }
            break;
            case "tuple":
            case "array":
            case "set":
            {
                assert node.getValue() != null;
                assert actual != null;

                var expectedNodes = (ResultNode[])node.getValue();
                var actualCollection = extractCollection(actual);

                assert expectedNodes.length == actualCollection.size();

                for(int i = 0; i != expectedNodes.length; i++){
                    assertNode(expectedNodes[i], actualCollection.get(i));
                }
            }
            break;
            default:
                var expected = node.getValue();

                assert !(expected instanceof ResultNode);

                if(expected == null) {
                    assert actual == null;
                    return;
                } else {
                    assert actual != null;
                }

                var reducer = reducers.stream()
                        .filter(v -> v.canReduce(expected.getClass(), actual.getClass()))
                        .findFirst();

                if(reducer.isPresent()) {
                    var reduced = reducer.get().reduce(expected, actual);
                    assertThat(reduced.getValue()).isEqualTo(reduced.getKey());
                    return;
                }

                if(actual instanceof Range && expected instanceof Range) {
                    var expRange = (Range<?>)expected;
                    var actRange = (Range<?>)actual;

                    assert expRange.doesIncludeLower() == actRange.doesIncludeLower();
                    assert expRange.doesIncludeUpper() == actRange.doesIncludeUpper();
                    assert expRange.isEmpty() == actRange.isEmpty();

                    // reduce lower and upper to compare
                    if(expRange.getLower() != null) {
                        assert actRange.getLower() != null;

                        var lowerReduced = reducers.stream().filter(v -> v.canReduce(expRange.getLower().getClass(), actRange.getLower().getClass())).findFirst();

                        if(lowerReduced.isPresent()) {
                            var reduced = lowerReduced.get().reduce(expRange.getLower(), actRange.getLower());
                            assertThat(reduced.getValue()).isEqualTo(reduced.getKey());
                        } else {
                            assertThat(actRange.getLower()).isEqualTo(expRange.getLower());
                        }
                    }

                    if(expRange.getUpper() != null) {
                        assert actRange.getUpper() != null;

                        var upperReduced = reducers.stream().filter(v -> v.canReduce(expRange.getUpper().getClass(), actRange.getUpper().getClass())).findFirst();

                        if(upperReduced.isPresent()) {
                            var reduced = upperReduced.get().reduce(expRange.getUpper(), actRange.getUpper());
                            assertThat(reduced.getValue()).isEqualTo(reduced.getKey());
                        } else {
                            assertThat(actRange.getUpper()).isEqualTo(expRange.getUpper());
                        }
                    }

                    return;
                }

                assertThat(actual).isEqualTo(expected);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> extractCollection(Object value) {
        assert value != null;

        var type = value.getClass();

        if(type.isArray()) {
            return List.of((Object[])value);
        }

        if(value instanceof Tuple) {
            return List.of(((Tuple)value).toArray());
        }

        if(value instanceof List)
            return (List<Object>)value;

        if(value instanceof Iterable<?>) {
            return Lists.newArrayList((Iterable<?>) value);
        }

        throw new AssertionFailedError("expected collection-like object, got " + type.getName());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractMap(Object value) {
        assert value != null;

        if(value instanceof Map) {
            return (Map<String, Object>)value;
        }

        var type = value.getClass();

        if(type.getPackage().getName().equals("com.gel.runtime")) {
            return Arrays.stream(type.getFields())
                    .map(v -> {
                        try {
                            return Map.entry(v.getName(), v.get(value));
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        throw new AssertionFailedError("expected object-like type, but got " + type.getName());
    }

    private static Map<ResultNode, Object> assertCollection(Object expected, Object actual, boolean reverseActual) {
        assert expected != null;
        assert actual != null;

        assert expected instanceof ResultNode[];

        var expectedArr = (ResultNode[])expected;

        var actualList = actual instanceof Iterable<?>
                ? Lists.newArrayList((Iterable<?>) actual)
                : actual.getClass().isArray()
                    ? new ArrayList<>(List.of((Object[]) actual))
                    : null;

        if(actualList == null) {
            throw new IllegalArgumentException("Unknown collection type " + actual.getClass().getName());
        }

        assert expectedArr.length == actualList.size();

        if(reverseActual) {
            Collections.reverse(actualList);
        }

        return IntStream.range(0, expectedArr.length)
                .mapToObj(i -> Map.entry(expectedArr[i], actualList.get(i)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
