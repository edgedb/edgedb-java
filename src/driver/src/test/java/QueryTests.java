import com.edgedb.driver.GelClientPool;
import com.edgedb.driver.annotations.EdgeDBLinkType;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.datatypes.MultiRange;
import com.edgedb.driver.datatypes.Range;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryTests {
    @EdgeDBType
    public static final class Links {
        public String a;
        public Links b;
        @EdgeDBLinkType(Links.class)
        public Collection<Links> c;
    }

    @Test
    public void testLinkProperties() throws Exception {
        try(var clientPool = new GelClientPool().withModule("tests")) {
            var result = clientPool
                    .execute(
                            "with a := (insert Links { a := 'A' } unless conflict on .a)," +
                            "b := (insert Links { a := 'B'} unless conflict on .a)," +
                            "c := (insert Links { a := 'C', c := b } unless conflict on .a)" +
                            "insert Links { a := 'D', c := { a, b, c }, b := c } unless conflict on .a")
                    .thenCompose(v ->
                            clientPool.queryRequiredSingle(
                                    Links.class,
                                    "select Links { a, c: { a, b, c }, b: { a, b, c } } filter .a = 'D'"
                            )
                    )
                    .toCompletableFuture().get();

            assertThat(result.a).isEqualTo("D");
            
            for (var linkClass : result.c) {
                assertThat(linkClass.getClass()).isEqualTo(Links.class); // verify its deserialized an actual object
            }
        }
    }

    @Test
    public void testMultiRanges() {
        try(var clientPool = new GelClientPool()) {
            var multiRange = new MultiRange<Long>(new ArrayList<Range<Long>>() {{
                add(Range.create(Long.class, -40L, -20L));
                add(Range.create(Long.class, 5L, 10L));
                add(Range.create(Long.class, 20L, 50L));
                add(Range.create(Long.class, 5000L, 5001L));
            }});

            var result = clientPool.queryRequiredSingle(
                    MultiRange.ofType(Long.class),
                    "SELECT <multirange<int64>>$arg",
                    new HashMap<>(){{
                        put("arg", multiRange);
                    }}
            ).toCompletableFuture().get();

            assertThat(result.length).isEqualTo(multiRange.length);

            for(int i = 0; i != multiRange.length; i++) {
                assertThat(result.get(i)).isEqualTo(multiRange.get(i));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EdgeDBType
    public static class TestDataContainer {
        public long a;
        public Long b;
        public int c;
        public Integer d;
    }

    @Test
    public void testPrimitives() {
        // primitives (long, int, etc.) differ from the class form (Long, Integer, etc.),
        // we test that we can deserialize both in a data structure.
        try(var clientPool = new GelClientPool()) {
            var result = clientPool.queryRequiredSingle(TestDataContainer.class, "select { a := 1, b := 2, c := <int32>3, d := <int32>4}")
                    .toCompletableFuture().get();

            assertThat(result.a).isEqualTo(1);
            assertThat(result.b).isEqualTo(2);
            assertThat(result.c).isEqualTo(3);
            assertThat(result.d).isEqualTo(4);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
