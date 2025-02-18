import com.edgedb.driver.GelClientPool;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.exceptions.GelException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtocolTests {
    private static final Logger logger = LoggerFactory.getLogger(ProtocolTests.class);

    @EdgeDBType
    public static class TestDatastructure {
        public UUID id;

        public String a;

        public String b;

        public String c;
    }

    /**
     * The goal is to test the contract logic in {@linkplain com.edgedb.driver.binary.PacketSerializer}, specifically
     * the decoder returned from the <b>createDecoder</b> function. To achieve this, we can query something that
     * returns either multiple data packets amounting up to >16k bytes, or a single data packet that is >16k bytes.
     */
    @Test
    public void testPacketContract() throws GelException, IOException, ExecutionException, InterruptedException {
        var clientPool = new GelClientPool().withModule("tests");

        // insert 1k items
        logger.info("Removing old data structures...");
        clientPool.execute("DELETE TestDatastructure")
                .toCompletableFuture().get();

        var results = new HashMap<UUID, String[]>();

        logger.info("Inserting 1000 items...");

        for(int i = 0; i != 1000; i++){
            var data = new String[] {
                    generateRandomString(),
                    generateRandomString(),
                    generateRandomString()
            };

            var result = clientPool.queryRequiredSingle(TestDatastructure.class, "INSERT TestDatastructure { a := <str>$a, b := <str>$b, c := <str>$c }", new HashMap<>(){{
                put("a", data[0]);
                put("b", data[1]);
                put("c", data[2]);
            }}).toCompletableFuture().get();

            results.put(result.id, data);
        }

        logger.info("Querying all items...");

        // assert the data can be read via binary and json
        var structures = clientPool.query(TestDatastructure.class, "SELECT TestDatastructure { id, a, b, c }")
                .toCompletableFuture().get();

        var json = clientPool.queryJson("SELECT TestDatastructure { id, a, b, c }")
                .toCompletableFuture().get();

        var structuresFromJson = List.of(new JsonMapper().readValue(json.getValue(), TestDatastructure[].class));

        assertStructuresMatch(structures, results);
        assertStructuresMatch(structuresFromJson, results);
    }

    private void assertStructuresMatch(List<TestDatastructure> source, Map<UUID, String[]> truth) {
        for(var structure : source) {
            assert structure != null;

            var expected = truth.get(structure.id);

            assertThat(structure.a).isEqualTo(expected[0]);
            assertThat(structure.b).isEqualTo(expected[1]);
            assertThat(structure.c).isEqualTo(expected[2]);

            logger.info("{} passed [a: {}, b: {}, c: {}]", structure.id, structure.a, structure.b, structure.c);
        }
    }

    private static String generateRandomString() {
        final var chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

        Random rand =new Random();
        StringBuilder res=new StringBuilder();
        for (int i = 0; i < 17; i++) {
            int randIndex=rand.nextInt(chars.length());
            res.append(chars.charAt(randIndex));
        }
        return res.toString();
    }
}
