import com.edgedb.driver.EdgeDBClient;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientTests {
    @Test
    public void testAutoClose() throws Exception {
        EdgeDBClient client;
        try {
            client = new EdgeDBClient();
        } catch (IOException | EdgeDBException e) {
            throw new RuntimeException(e);
        }

        var result = client.querySingle(String.class, "SELECT 'Hello, Java'")
                        .toCompletableFuture().get();

        assertThat(result).isEqualTo("Hello, Java");
        assertThat(client.getClientCount()).isEqualTo(1);

        client.close();

        assertThat(client.getClientCount()).isEqualTo(0);
    }
}
