import com.edgedb.driver.GelClientPool;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientTests {
    @Test
    public void testAutoClose() throws Exception {
        GelClientPool clientPool;
        try {
            clientPool = new GelClientPool();
        } catch (IOException | EdgeDBException e) {
            throw new RuntimeException(e);
        }

        var result = clientPool.querySingle(String.class, "SELECT 'Hello, Java'")
                        .toCompletableFuture().get();

        assertThat(result).isEqualTo("Hello, Java");
        assertThat(clientPool.getClientCount()).isEqualTo(1);

        clientPool.close();

        assertThat(clientPool.getClientCount()).isEqualTo(0);
    }
}
