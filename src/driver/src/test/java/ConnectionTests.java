import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.TLSSecurityMode;
import com.edgedb.driver.EdgeDBConnection.ConfigureFunction;
import com.edgedb.driver.EdgeDBConnection.WaitTime;
import com.edgedb.driver.abstractions.SystemProvider;
import com.edgedb.driver.internal.BaseDefaultSystemProvider;
import com.edgedb.driver.util.ConfigUtils;
import com.edgedb.driver.util.ConfigUtils.ResolvedField;
import com.edgedb.driver.exceptions.ConfigurationException;

import org.assertj.core.api.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"SpellCheckingInspection"})
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
        expect(
            parse(
                "edgedb://user3:123123@localhost:5555/abcdef",
                new HashMap<>() {{
                    put("EDGEDB_USER", "user");
                    put("EDGEDB_DATABASE", "testdb");
                    put("EDGEDB_PASSWORD", "passw");
                    put("EDGEDB_HOST", "host");
                    put("EDGEDB_PORT", "123");
                }}
            ),
            new EdgeDBConnection() {{
                setHostname("localhost");
                setPort(5555);
                setUsername("user3");
                setPassword("123123");
                setDatabase("testdb"); // possibly wrong but this test will be removed
                setTLSSecurity(TLSSecurityMode.STRICT);
            }}
        );
    }

    @Test
    public void dsnOnly() throws ConfigurationException {
        expect(
            parse(
                "edgedb://user3:123123@localhost:5555/abcdef"
            ),
            new EdgeDBConnection() {{
                setHostname("localhost");
                setPort(5555);
                setUsername("user3");
                setPassword("123123");
                setDatabase("abcdef");
                setTLSSecurity(TLSSecurityMode.STRICT);
            }}
        );
    }

    @Test
    public void dsnWithMultipleHosts() {
        expect(
            parse(
                "edgedb://user@host1,host2/db"
            ),
            ConfigurationException.class,
            "Invalid host: \"host1,host2\", DSN cannot contain more than one host"
        );
    }

    @Test
    public void dsnWithMultipleHostsAndPorts() {
        expect(
            parse(
            "edgedb://user@host1:1111,host2:2222/db"
            ),
            ConfigurationException.class,
            "Invalid DSN: Could not parse host/port"
        );
    }

    @Test
    public void envvarsWithMultipleHostsAndPorts() {
        expect(
            parse(
                new HashMap<>() {{
                    put("EDGEDB_HOST", "host1:1111,host2:2222");
                    put("EDGEDB_USER", "foo");
                }}
            ),
            ConfigurationException.class,
            "Environment variable 'EDGEDB_HOST' cannot contain more than one host"
        );
    }

    @Test
    public void queryParametersWithMultipleHostsAndPorts() {
        expect(
            parse(
                "edgedb:///db?host=host1:1111,host2:2222",
                new HashMap<>() {{
                    put("EDGEDB_USER", "foo");
                }}
            ),
            ConfigurationException.class,
            "Invalid host: \"host1:1111,host2:2222\", DSN cannot contain more than one host"
        );
    }

    @Test
    public void dsnWithUnixSocket() {
        expect(
            parse(
                "edgedb:///dbname?host=/unix_sock/test&user=spam"
            ),
            ConfigurationException.class,
            "Invalid host: \"/unix_sock/test\", unix socket paths not supported"
        );
    }

    @Test
    public void dsnRequiresEdgeDBSchema() {
        expect(
            parse(
                "pq:///dbname?host=/unix_sock/test&user=spam"
            ),
            ConfigurationException.class,
            "Invalid DSN scheme. Expected \"gel\" but got \"pq\""
        );
    }

    @Test
    public void dsnQueryParameterWithUnixSocket() {
        expect(
            parse(
                "edgedb://user@?port=56226&host=%2Ftmp"
            ),
            ConfigurationException.class,
            "Invalid DSN: \"edgedb://user@?port=56226&host=%2Ftmp\""
        );
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
        assertThat(result.exception).hasMessage(message);
        assertThat(result.exception).isInstanceOf(exception);
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

    private static Result parse(
        String conn,
        EdgeDBConnection.ConfigureFunction conf,
        Map<String, String> env
    ) {
        try {
            // Use reflection to access internal parse method
            return new Result((EdgeDBConnection) connectionParse().invoke(
                null,
                conn,
                conf,
                false,
                new MockProvider(env)
            ));
        } catch (InvocationTargetException e) {
            return new Result((Exception) e.getCause());
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

    private static Method connectionParse() throws NoSuchMethodException {
        Method method = EdgeDBConnection.class.getDeclaredMethod(
            "_parse",
            String.class,
            ConfigureFunction.class,
            boolean.class,
            SystemProvider.class
        );
        method.setAccessible(true);
        return method;
    }

    private static class MockProvider extends BaseDefaultSystemProvider {
        Map<String, String> env;
        MockProvider() {}
        MockProvider(Map<String, String> env) {
            this.env = env;
        }

        @Override
        public @Nullable String getEnvVariable(@NotNull String name)
        {
            if (env != null) {
                return env.getOrDefault(name, null);
            }
            return super.getEnvVariable(name);
        }
    }

    //#region Wait until available

    @ParameterizedTest
    @ArgumentsSource(ValidWaitUntilAvailableArgumentsProvider.class)
    public void validWaitUntilAvailable(
        String input,
        double expectedSeconds
    ) throws ConfigurationException {
        ResolvedField<WaitTime> actual = ConfigUtils.parseWaitUntilAvailable(input);
        assertNotNull(actual.getValue());
        assertTrue(
            Math.abs(actual.getValue().valueInUnits(TimeUnit.MICROSECONDS) * 1e-6 - expectedSeconds) < 0.5e-6
        );
    }

    static class ValidWaitUntilAvailableArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of(" 1s ", 1),
                Arguments.of(" 1s", 1),
                Arguments.of("-0s", 0),
                Arguments.of("-1.0h", -3600),
                Arguments.of("-1.0hour", -3600),
                Arguments.of("-1.0hours", -3600),
                Arguments.of("-1.0m", -60),
                Arguments.of("-1.0minute", -60),
                Arguments.of("-1.0minutes", -60),
                Arguments.of("-1.0ms", -0.001),
                Arguments.of("-1.0s", -1),
                Arguments.of("-1.0second", -1),
                Arguments.of("-1.0seconds", -1),
                Arguments.of("-1.0us", -0.000001),
                Arguments.of("-1h", -3600),
                Arguments.of("-1hour", -3600),
                Arguments.of("-1hours", -3600),
                Arguments.of("-1m", -60),
                Arguments.of("-1minute", -60),
                Arguments.of("-1minutes", -60),
                Arguments.of("-1ms", -0.001),
                Arguments.of("-1s", -1),
                Arguments.of("-1second", -1),
                Arguments.of("-1seconds", -1),
                Arguments.of("-1us", -0.000001),
                Arguments.of("-2h 60m 3600s", 0),
                Arguments.of("-\t2\thour\t60\tminute\t3600\tsecond", 0),
                Arguments.of(".1h", 360),
                Arguments.of(".1hour", 360),
                Arguments.of(".1hours", 360),
                Arguments.of(".1m", 6),
                Arguments.of(".1minute", 6),
                Arguments.of(".1minutes", 6),
                Arguments.of(".1ms", 0.0001),
                Arguments.of(".1s", 0.1),
                Arguments.of(".1second", 0.1),
                Arguments.of(".1seconds", 0.1),
                Arguments.of(".1us", 0.0000001),
                Arguments.of("1   hour 60  minute -   7200   second", 0),
                Arguments.of("1   hours 60  minutes -   7200   seconds", 0),
                Arguments.of("1.0h", 3600),
                Arguments.of("1.0hour", 3600),
                Arguments.of("1.0hours", 3600),
                Arguments.of("1.0m", 60),
                Arguments.of("1.0minute", 60),
                Arguments.of("1.0minutes", 60),
                Arguments.of("1.0ms", 0.001),
                Arguments.of("1.0s", 1),
                Arguments.of("1.0second", 1),
                Arguments.of("1.0seconds", 1),
                Arguments.of("1.0us", 0.000001),
                Arguments.of("1h -120m 3600s", 0),
                Arguments.of("1h -120m3600s", 0),
                Arguments.of("1h 60m -7200s", 0),
                Arguments.of("1h", 3600),
                Arguments.of("1hour", 3600),
                Arguments.of("1hours -120minutes 3600seconds", 0),
                Arguments.of("1hours", 3600),
                Arguments.of("1m", 60),
                Arguments.of("1minute", 60),
                Arguments.of("1minutes", 60),
                Arguments.of("1ms", 0.001),
                Arguments.of("1s ", 1),
                Arguments.of("1s", 1),
                Arguments.of("1s\t", 1),
                Arguments.of("1second", 1),
                Arguments.of("1seconds", 1),
                Arguments.of("1us", 0.000001),
                Arguments.of("2  h  46  m  39  s", 9999),
                Arguments.of("2  hour  46  minute  39  second", 9999),
                Arguments.of("2  hours  46  minutes  39  seconds", 9999),
                Arguments.of("2.0  h  46.0  m  39.0  s", 9999),
                Arguments.of("2.0  hour  46.0  minute  39.0  second", 9999),
                Arguments.of("2.0  hours  46.0  minutes  39.0  seconds", 9999),
                Arguments.of("2.0h 46.0m 39.0s", 9999),
                Arguments.of("2.0h46.0m39.0s", 9999),
                Arguments.of("2.0hour 46.0minute 39.0second", 9999),
                Arguments.of("2.0hours 46.0minutes 39.0seconds", 9999),
                Arguments.of("2h 46m 39s", 9999),
                Arguments.of("2h46m39s", 9999),
                Arguments.of("2hour 46minute 39second", 9999),
                Arguments.of("2hours 46minutes 39seconds", 9999),
                Arguments.of("39.0\tsecond 2.0  hour  46.0  minute", 9999),
                Arguments.of("PT", 0),
                Arguments.of("PT-.1", -360),
                Arguments.of("PT-.1H", -360),
                Arguments.of("PT-.1M", -6),
                Arguments.of("PT-.1S", -0.1),
                Arguments.of("PT-0.000001S", -0.000001),
                Arguments.of("PT-0S", 0),
                Arguments.of("PT-1", -3600),
                Arguments.of("PT-1.", -3600),
                Arguments.of("PT-1.0", -3600),
                Arguments.of("PT-1.0H", -3600),
                Arguments.of("PT-1.0M", -60),
                Arguments.of("PT-1.0S", -1),
                Arguments.of("PT-1.H", -3600),
                Arguments.of("PT-1.M", -60),
                Arguments.of("PT-1.S", -1),
                Arguments.of("PT-1H", -3600),
                Arguments.of("PT-1M", -60),
                Arguments.of("PT-1S", -1),
                Arguments.of("PT.1", 360),
                Arguments.of("PT.1H", 360),
                Arguments.of("PT.1M", 6),
                Arguments.of("PT.1S", 0.1),
                Arguments.of("PT0.000001S", 0.000001),
                Arguments.of("PT0S", 0),
                Arguments.of("PT1", 3600),
                Arguments.of("PT1.", 3600),
                Arguments.of("PT1.0", 3600),
                Arguments.of("PT1.0H", 3600),
                Arguments.of("PT1.0M", 60),
                Arguments.of("PT1.0M", 60),
                Arguments.of("PT1.0S", 1),
                Arguments.of("PT1.H", 3600),
                Arguments.of("PT1.M", 60),
                Arguments.of("PT1.S", 1),
                Arguments.of("PT1H", 3600),
                Arguments.of("PT1M", 60),
                Arguments.of("PT1S", 1),
                Arguments.of("PT2.0H46.0M39.0S", 9999),
                Arguments.of("PT2H46M39S", 9999),
                Arguments.of("\t-\t2\thours\t60\tminutes\t3600\tseconds\t", 0),
                Arguments.of("\t1s", 1),
                Arguments.of("\t1s\t", 1)
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        " ",
        " PT1S",
        "",
        "-.1 s",
        "-.1s",
        "-.5 second",
        "-.5 seconds",
        "-.5second",
        "-.5seconds",
        "-.s",
        "-1.s",
        ".s",
        ".seconds",
        "1.s",
        "1h-120m3600s",
        "1hour-120minute3600second",
        "1hours-120minutes3600seconds",
        "1hours120minutes3600seconds",
        "2.0hour46.0minutes39.0seconds",
        "2.0hours46.0minutes39.0seconds",
        "20 hours with other stuff should not be valid",
        "20 minutes with other stuff should not be valid",
        "20 ms with other stuff should not be valid",
        "20 seconds with other stuff should not be valid",
        "20 us with other stuff should not be valid",
        "2hour46minute39second",
        "2hours46minutes39seconds",
        "3 hours is longer than 10 seconds",
        "P-.D",
        "P-D",
        "PD",
        "PT.S",
        "PT1S ",
        "\t",
        "not a duration",
        "s",
    })
    public void invalidWaitUntilAvailable(
        String input
    ) throws ConfigurationException {
        ResolvedField<WaitTime> actual = ConfigUtils.parseWaitUntilAvailable(input);
        assertNotNull(actual.getError());
    }

    //#endregion
}
