import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.TLSSecurityMode;
import com.edgedb.driver.exceptions.ConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"SpellCheckingInspection", "unchecked"})
public class ConnectionTests {

    @Test
    public void hostAndUser() throws ConfigurationException {
        expect(parse(c -> {
            c.withUser("user");
            c.withHostname("localhost");
        }), new EdgeDBConnection(){{
            setHostname("localhost");
            setPort(5656);
            setUsername("user");
            setDatabase("edgedb");
            setTLSSecurity(TLSSecurityMode.STRICT);
        }});
    }

    @Test
    @SuppressWarnings("SpellCheckingInspection")
    public void allEnvVars() throws ConfigurationException {
        expect(parse(new HashMap<>(){{
            put("EDGEDB_USER", "user");
            put("EDGEDB_DATABASE", "testdb");
            put("EDGEDB_PASSWORD", "passw");
            put("EDGEDB_HOST", "host");
            put("EDGEDB_PORT", "123");
        }}), new EdgeDBConnection(){{
            setHostname("host");
            setPort(123);
            setUsername("user");
            setPassword("passw");
            setDatabase("testdb");
            setTLSSecurity(TLSSecurityMode.STRICT);
        }});
    }

    @Test
    public void optionsBeforeEnv() throws ConfigurationException {
        expect(parse(c -> {
            c.withHostname("host2");
            c.withPort(456);
            c.withUser("user2");
            c.withPassword("passw2");
            c.withDatabase("db2");
        }, new HashMap<>() {{
            put("EDGEDB_USER", "user");
            put("EDGEDB_DATABASE", "testdb");
            put("EDGEDB_PASSWORD", "passw");
            put("EDGEDB_HOST", "host");
            put("EDGEDB_PORT", "123");
        }}), new EdgeDBConnection() {{
            setHostname("host2");
            setPort(456);
            setUsername("user2");
            setPassword("passw2");
            setDatabase("db2");
            setTLSSecurity(TLSSecurityMode.STRICT);
        }});
    }

    @Test
    public void dsnBeforeEnv() throws ConfigurationException {
        expect(parse(
                "edgedb://user3:123123@localhost:5555/abcdef",
                new HashMap<>() {{
                    put("EDGEDB_USER", "user");
                    put("EDGEDB_DATABASE", "testdb");
                    put("EDGEDB_PASSWORD", "passw");
                    put("EDGEDB_HOST", "host");
                    put("EDGEDB_PORT", "123");
                }}
        ), new EdgeDBConnection() {{
            setHostname("localhost");
            setPort(5555);
            setUsername("user3");
            setPassword("123123");
            setDatabase("abcdef");
            setTLSSecurity(TLSSecurityMode.STRICT);
        }});
    }

    @Test
    public void dsnOnly() throws ConfigurationException {
        expect(parse(
                "edgedb://user3:123123@localhost:5555/abcdef"
        ), new EdgeDBConnection() {{
            setHostname("localhost");
            setPort(5555);
            setUsername("user3");
            setPassword("123123");
            setDatabase("abcdef");
            setTLSSecurity(TLSSecurityMode.STRICT);
        }});
    }

    @Test
    public void dsnWithMultipleHosts() {
        expect(parse(
                "edgedb://user@host1,host2/db"
        ), ConfigurationException.class, "DSN cannot contain more than one host");
    }

    @Test
    public void dsnWithMultipleHostsAndPorts() {
        expect(parse(
                "edgedb://user@host1:1111,host2:2222/db"
        ), ConfigurationException.class, "DSN cannot contain more than one host");
    }

    @Test
    public void envvarsWithMultipleHostsAndPorts() {
        expect(parse(
                new HashMap<>() {{
                    put("EDGEDB_HOST", "host1:1111,host2:2222");
                    put("EDGEDB_USER", "foo");
                }}
        ), ConfigurationException.class, "Environment variable 'EDGEDB_HOST' cannot contain more than one host");
    }

    @Test
    public void queryParametersWithMultipleHostsAndPorts() {
        expect(parse(
                "edgedb:///db?host=host1:1111,host2:2222",
                new HashMap<>() {{
                    put("EDGEDB_USER", "foo");
                }}
        ), ConfigurationException.class, "DSN cannot contain more than one host");
    }

    @Test
    public void multipleCompoundOptions() {
        expect(parse(
                "edgedb:///db",
                c -> c.withHostname("host1"),
                new HashMap<>() {{
                    put("EDGEDB_USER", "foo");
                }}
        ), ConfigurationException.class, "Cannot specify DSN and 'Hostname'; they are mutually exclusive");
    }

    @Test
    public void dsnWithUnixSocket() {
        expect(parse(
                "edgedb:///dbname?host=/unix_sock/test&user=spam"
        ), ConfigurationException.class, "Cannot use UNIX socket for 'Hostname'");
    }

    @Test
    public void dsnRequiresEdgeDBSchema() {
        expect(parse(
                "pq:///dbname?host=/unix_sock/test&user=spam"
        ), ConfigurationException.class, "DSN schema 'edgedb' expected but got 'pq'");
    }

    @Test
    public void dsnQueryParameterWithUnixSocket() {
        expect(parse(
                "edgedb://user@?port=56226&host=%2Ftmp"
        ), ConfigurationException.class, "Cannot use UNIX socket for 'Hostname'");
    }

    @Test
    public void testConnectionFormat() throws ConfigurationException, IOException {
        var conn = EdgeDBConnection.fromDSN("edgedb://user3:123123@localhost:5555/abcdef");

        assertThat(conn.toString()).isEqualTo("edgedb://user3:123123@localhost:5555/abcdef");
    }

    private static void expect(Result result, EdgeDBConnection expected) {
        var actual = result.connection;

        assertThat(actual).isNotNull();

        assertThat(actual.getUsername()).isEqualTo(expected.getUsername());
        assertThat(actual.getPassword()).isEqualTo(expected.getPassword());
        assertThat(actual.getHostname()).isEqualTo(expected.getHostname());
        assertThat(actual.getPort()).isEqualTo(expected.getPort());
        assertThat(actual.getDatabase()).isEqualTo(expected.getDatabase());
        assertThat(actual.getTLSCertificateAuthority()).isEqualTo(expected.getTLSCertificateAuthority());
        assertThat(actual.getTLSSecurity()).isEqualTo(expected.getTLSSecurity());
    }

    private static <T extends Throwable> void expect(Result result, Class<T> exception, String message) {
        assertThat(result.exception).isNotNull();
        assertThat(result.exception).isInstanceOf(exception);
        assertThat(result.exception.getMessage()).isEqualTo(message);
    }

    private static Result parse(String conn) {
        return parse(conn, null, null);
    }

    private static Result parse(String conn, EdgeDBConnection.ConfigureFunction conf) {
        return parse(conn, conf, null);
    }

    private static Result parse(String conn, Map<String, String> env) {
        return parse(conn, null, env);
    }

    private static Result parse(EdgeDBConnection.ConfigureFunction conf, Map<String, String> env) {
        return parse(null, conf, env);
    }

    private static Result parse(Map<String, String> env) {
        return parse(null, null, env);
    }

    private static Result parse(EdgeDBConnection.ConfigureFunction conf) {
        return parse(null, conf, null);
    }

    private static Result parse(String conn, EdgeDBConnection.ConfigureFunction conf, Map<String, String> env) {
        try {
            return new Result(EdgeDBConnection.parse(conn, conf, false, env == null ? System::getenv : env::get));
        } catch (Exception e) {
            return new Result(e);
        }
    }

    private static final class Result {
        public final @Nullable EdgeDBConnection connection;
        public final @Nullable Exception exception;

        public Result(@NotNull EdgeDBConnection connection) {
            this.connection = connection;
            this.exception = null;
        }

        public Result(@NotNull Exception exception) {
            this.connection = null;
            this.exception = exception;
        }
    }
}
