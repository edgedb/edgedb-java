import com.edgedb.driver.EdgeDBClient;
import com.edgedb.driver.annotations.EdgeDBType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryTests {

    @EdgeDBType
    public static class TestDataContainer {
        public long a;
        public Long b;
        public int c;
        public Integer d;
    }

    @Test
    public void TestPrimitives() {
        // primitives (long, int, etc.) differ from the class form (Long, Integer, etc.),
        // we test that we can deserialize both in a data structure.
        try(var client = new EdgeDBClient()) {
            var result = client.queryRequiredSingle(TestDataContainer.class, "select { a := 1, b := 2, c := <int32>3, d := <int32>4}")
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
