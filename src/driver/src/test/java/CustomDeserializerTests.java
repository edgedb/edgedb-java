import com.edgedb.driver.EdgeDBClient;
import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBLinkType;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomDeserializerTests {

    @EdgeDBType
    public static final class Links {
        private String namesHereAreIrrelevant;
        private Links sinceTheCustomDeserializer;
        private Collection<Links> shouldMapNames;

        @EdgeDBDeserializer
        public Links(
                @EdgeDBName("a") String a,
                @EdgeDBName("b") Links b,
                @EdgeDBName("c") @EdgeDBLinkType(Links.class) Collection<Links> c
        ) {
            this.namesHereAreIrrelevant = a;
            this.sinceTheCustomDeserializer = b;
            this.shouldMapNames = c;
        }
    }

    @Test
    public void testCustomDeserializerParameterAnnotations() throws Exception {
        try(var client = new EdgeDBClient().withModule("tests")) {
            var result = client.queryRequiredSingle(
                    Links.class,
                    "with test1 := (insert Links { a := '123' } unless conflict on .a else (select Links))," +
                    "test2 := (insert Links { a := '456', b := test1 } unless conflict on .a else (select Links))," +
                    "test3 := (insert Links { a := '789', b := test2, c := { test1, test2 }} unless conflict on .a else (select Links)) " +
                    "select test3 {a, b: {a, b, c }, c: {a, b, c}}"
            ).toCompletableFuture().get();

            assertThat(result.namesHereAreIrrelevant).isEqualTo("789");
            assertThat(result.shouldMapNames.size()).isEqualTo(2);

            for(var link : result.shouldMapNames) {
                assertThat(link.getClass()).isEqualTo(Links.class);
            }

            assertThat(result.sinceTheCustomDeserializer).isNotNull();
        }
    }
}
