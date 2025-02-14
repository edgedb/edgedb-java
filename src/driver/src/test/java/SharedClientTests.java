import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Convert;

import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.EdgeDBConnection.WaitTime;
import com.edgedb.driver.TLSSecurityMode;
import com.edgedb.driver.abstractions.OSType;
import com.edgedb.driver.abstractions.SystemProvider;
import com.edgedb.driver.exceptions.ConfigurationException;
import com.edgedb.driver.internal.BaseDefaultSystemProvider;
import com.edgedb.driver.internal.DefaultSystemProvider;
import com.edgedb.driver.util.ConfigUtils;
import com.edgedb.driver.util.HexUtils;
import com.edgedb.driver.util.ConfigUtils.ResolvedField;
import com.edgedb.driver.util.JsonUtils.AsStringDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.IntNode;

import net.bytebuddy.implementation.bytecode.Throw;

public class SharedClientTests {

    @Test
    public void testConnectParams() throws IOException {

        InputStream testCaseStream = ClassLoader.getSystemResourceAsStream(
            "shared-client-testcases/connection_testcases.json"
        );

        List<TestCase> testCases = TestCase.mapper.readValue(testCaseStream, new TypeReference<List<TestCase>>(){});


        for (TestCase testCase : testCases) {
            if (testCase.fileSystem != null
                && (
                    !(
                        testCase.platform != null
                        && testCase.platform.equals("windows")
                        && (new DefaultSystemProvider()).isOSPlatform(OSType.WINDOWS)
                    )
                    || !(
                        testCase.platform != null
                        && testCase.platform.equals("macos")
                        && (new DefaultSystemProvider()).isOSPlatform(OSType.OSX)
                    )
                )
            ) {
                // skipping unsupported platform test
                continue;
            }

            if ((testCase.result == null) == (testCase.error == null)) {
                throw new IOException("invalid test case: either \"result\" or \"error\" key has to be specified");
            }

            TestResult result = ParseConnection(testCase);

            if (testCase.result != null) {
                assertSameConnection(testCase.name, result, testCase.result);
            }
            else if (testCase.error != null) {
                assertSameException(testCase.name, result, testCase.error);
            }
        }
    }

    private static class TestResult {
        public @Nullable EdgeDBConnection connection;
        public @Nullable Exception error;

        public static TestResult valid(EdgeDBConnection connection) {
            TestResult result = new TestResult();
            result.connection = connection;
            return result;
        }

        public static TestResult invalid(Exception error) {
            TestResult result = new TestResult();
            result.error = error;
            return result;
        }
    }

    private static TestResult ParseConnection(TestCase testCase) {
        try {
            EdgeDBConnection.Builder builder = new EdgeDBConnection.Builder();

            if (testCase.options != null) {
                TestCase.OptionsData options = testCase.options;

                if (options.instance != null) {
                    builder = builder.withInstance(options.instance);
                }
                if (options.dsn != null) {
                    builder = builder.withDsn(options.dsn);
                }
                if (options.credentials != null) {
                    builder = builder.withCredentials(options.credentials);
                }
                if (options.credentialsFile != null) {
                    builder = builder.withCredentialsFile(Path.of(options.credentialsFile));
                }
                if (options.host != null) {
                    builder = builder.withHost(options.host);
                }
                if (options.port != null) {
                    try {
                        Integer port = Integer.parseInt(options.port);
                        builder = builder.withPort(port);
                    }
                    catch (NumberFormatException e) {
                        return TestResult.invalid(
                            new ConfigurationException(String.format(
                                "Invalid port: \"%s\", not an integer",
                                options.port
                            ))
                        );
                    }
                }
                if (options.database != null) {
                    builder = builder.withDatabase(options.database);
                }
                if (options.branch != null) {
                    builder = builder.withBranch(options.branch);
                }
                if (options.user != null) {
                    builder = builder.withUser(options.user);
                }
                if (options.password != null) {
                    builder = builder.withPassword(options.password);
                }
                if (options.secretKey != null) {
                    builder = builder.withSecretKey(options.secretKey);
                }
                if (options.tlsCA != null) {
                    builder = builder.withTLSCertificateAuthority(options.tlsCA);
                }
                if (options.tlsCAFile != null) {
                    builder = builder.withTLSCertificateAuthorityFile(Path.of(options.tlsCAFile));
                }
                if (options.tlsSecurity != null) {
                    ResolvedField<TLSSecurityMode> tlsSecurity = ConfigUtils.parseTLSSecurityMode(
                        options.tlsSecurity
                    );
                    if (tlsSecurity.getValue() != null) {
                        builder = builder.withTLSSecurity(tlsSecurity.getValue());
                    }
                    else {
                        throw tlsSecurity.getError();
                    }
                }
                if (options.tlsServerName != null) {
                    builder = builder.withTLSServerName(options.tlsServerName);
                }
                if (options.waitUntilAvailable != null) {
                    builder = builder.withWaitUntilAvailable(options.waitUntilAvailable);
                }
                if (options.serverSettings != null) {
                    builder = builder.withServerSettings(options.serverSettings);
                }
            }

            // Use reflection to access private build method
            Method method = EdgeDBConnection.class.getDeclaredMethod(
                "fromBuilder",
                EdgeDBConnection.Builder.class,
                SystemProvider.class
            );
            method.setAccessible(true);

            try {
                EdgeDBConnection connection = (EdgeDBConnection)method.invoke(
                    null,
                    builder,
                    new MockProvider(testCase)
                );

                return TestResult.valid(connection);
            }
            catch (InvocationTargetException e) {
                return TestResult.invalid((Exception)e.getTargetException());
            }
        }
        catch (ConfigurationException e) {
            return TestResult.invalid(e);
        }
        catch (Exception e) {
            return TestResult.invalid(new Exception(String.format(
                "Something went really wrong: %s",
                prettyError(e)
            )));
        }
    }

    private static String prettyError(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(e.getMessage());
        e.printStackTrace(pw);
        return sw.toString();
    }

    //#region Test Assertions

    private static void assertSameConnection(
        @NotNull String testName,
        @NotNull TestResult result,
        @NotNull TestCase.ExpectedResult expectedResult
    ) {
        @NotNull String expectedHostname = expectedResult.address != null
            ? expectedResult.address.get(0)
            : "localhost";
        @NotNull Integer expectedPort = expectedResult.address != null
            ? Integer.parseInt(expectedResult.address.get(1))
            : 5656;
        @NotNull String expectedDatabase = expectedResult.database;
        @NotNull String expectedBranch = expectedResult.branch;
        @NotNull String expectedUsername = expectedResult.user;
        @NotNull String expectedPassword = expectedResult.password != null
            ? expectedResult.password
            : "";
        @Nullable String expectedSecretKey = expectedResult.secretKey;
        @Nullable String expectedTLSCertificateAuthority = expectedResult.tlsCAData;
        TLSSecurityMode expectedTLSSecurity = expectedResult.tlsSecurity != null
            ? TLSSecurityMode.fromString(expectedResult.tlsSecurity, true)
            : TLSSecurityMode.DEFAULT;
        if (expectedTLSSecurity == TLSSecurityMode.DEFAULT) {
            expectedTLSSecurity = TLSSecurityMode.STRICT;
        }
        @Nullable String expectedTLSServerName = expectedResult.tlsServerName;
        @NotNull WaitTime expectedWaitUntilAvailable = expectedResult.waitUntilAvailable != null
            ? ConfigUtils.parseWaitUntilAvailable(expectedResult.waitUntilAvailable).getValue()
            : new WaitTime(30l, TimeUnit.SECONDS);

        assertNull(
            result.error,
            String.format(
                "%s: %s",
                testName,
                (result.error != null ? prettyError(result.error) : "")
            )
        );
        assertNotNull(result.connection, testName);

        EdgeDBConnection actual = result.connection;

        assertEquals(expectedHostname, actual.getHostname(), testName);
        assertEquals(expectedPort, actual.getPort(), testName);
        assertEquals(expectedDatabase, actual.getDatabase(), testName);
        assertEquals(expectedBranch, actual.getBranch(), testName);
        assertEquals(expectedUsername, actual.getUsername(), testName);
        assertEquals(expectedPassword, actual.getPassword(), testName);
        assertEquals(expectedSecretKey, actual.getSecretKey(), testName);
        assertEquals(expectedTLSCertificateAuthority, actual.getTLSCertificateAuthority(), testName);
        assertEquals(expectedTLSSecurity, actual.getTLSSecurity(), testName);
        assertEquals(expectedTLSServerName, actual.getTLSServerName(), testName);
        assertEquals(
            expectedWaitUntilAvailable.valueInUnits(TimeUnit.MICROSECONDS),
            actual.getWaitUntilAvailable().valueInUnits(TimeUnit.MICROSECONDS),
            testName
        );
        assertTrue(expectedResult.serverSettings.equals(actual.getServerSettings()), testName);
    }

    private static void assertSameException(
        @NotNull String testName,
        @NotNull TestResult result,
        @NotNull TestCase.ExpectedError expectedError
    ) {
        ErrorMatch errorMatch = _errorMapping.get(expectedError.type);

        assertNull(result.connection, testName);
        assertNotNull(result.error, testName);
        Exception actual = result.error;

        assertEquals(
            errorMatch.type,
            actual.getClass(),
            String.format(
                "%s: %s",
                testName,
                prettyError(actual)
            )
        );
        assertTrue(
            errorMatch.text.matcher(actual.getMessage()).find(),
            String.format(
                "%s: Exception message \"%s\" does not match pattern \"%s\"\n",
                testName,
                actual.getMessage(),
                errorMatch.text.toString()
            )
        );
    }

    private static class ErrorMatch {
        public final @NotNull Type type;
        public final @NotNull Pattern text;
        public ErrorMatch(@NotNull Type type, @NotNull Pattern text) {
            this.type = type;
            this.text = text;
        }
    }
    private static final Map<String, ErrorMatch> _errorMapping = Map.ofEntries(
        entry(
            "credentials_file_not_found",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("cannot read credentials")
            )
        ),
        entry(
            "project_not_initialised",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("Found `\\w+.toml` but the project is not initialized")
            )
        ),
        entry(
            "no_options_or_toml",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("No `gel.toml` found and no connection options specified")
            )
        ),
        entry(
            "invalid_credentials_file",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("Invalid CredentialsFile")
            )
        ),
        entry(
            "invalid_dsn_or_instance_name",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("Invalid (?:DSN|instance name)")
            )
        ),
        entry(
            "invalid_instance_name",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("invalid instance name")
            )
        ),
        entry(
            "invalid_dsn",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("Invalid DSN")
            )
        ),
        entry(
            "unix_socket_unsupported",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("unix socket paths not supported")
            )
        ),
        entry(
            "invalid_host",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("Invalid host")
            )
        ),
        entry(
            "invalid_port",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("Invalid port")
            )
        ),
        entry(
            "invalid_user",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("Invalid user")
            )
        ),
        entry(
            "invalid_database",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("Invalid database")
            )
        ),
        entry(
            "multiple_compound_env",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile(
                    "Cannot have more than one of the following connection environment variables"
                )
            )
        ),
        entry(
            "multiple_compound_opts",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile(
                    "Connection options cannot have more than one of the following values"
                )
            )
        ),
        entry(
            "exclusive_options",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("are mutually exclusive")
            )
        ),
        entry(
            "env_not_found",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("environment variable \".*\" doesn\'t exist")
            )
        ),
        entry(
            "file_not_found",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("could not find file")
            )
        ),
        entry(
            "invalid_tls_security",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("Invalid TLS Security|\\w+ must be strict when \\w+ is strict")
            )
        ),
        entry(
            "invalid_secret_key",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("Invalid secret key")
            )
        ),
        entry(
            "secret_key_not_found",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("Cannot connect to cloud instances without secret key")
            )
        ),
        entry(
            "docker_tcp_port",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("\\w+_PORT in \"tcp://host:port\" format, so will be ignored")
            )
        ),
        entry(
            "gel_and_edgedb",
            new ErrorMatch(
                ConfigurationException.class,
                Pattern.compile("Both GEL_\\w+ and EDGEDB_\\w+ are set; EDGEDB_\\w+ will be ignored")
            )
        )
    );

    //#endregion

    //#region MockSystemProvider

    private static class MockProvider extends BaseDefaultSystemProvider {

        private static final MessageDigest SHA1;

        static {
            try {
                SHA1 = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        final @Nullable Path homeDir;
        final @Nullable Path currentDir;
        final @Nullable Map<String, String> envVars;
        final HashMap<String, String> files;

        public List<String> warnings;

        MockProvider(@NotNull TestCase testCase) throws IOException {
            this.homeDir = testCase.fileSystem != null ? Path.of(testCase.fileSystem.homeDir) : null;
            this.currentDir = testCase.fileSystem != null ? Path.of(testCase.fileSystem.currentDir) : null;
            this.envVars = testCase.envVars;
            this.files = cacheFiles(testCase);
            this.warnings = new ArrayList<String>();
        }

        private HashMap<String, String> cacheFiles(@NotNull TestCase testCase) throws IOException {
            if (testCase.fileSystem == null || testCase.fileSystem.files == null) {
                return new HashMap<String, String>();
            }

            HashMap<String, String> result = new HashMap<String, String>();

            for (Entry<String, TestCase.File> entry : testCase.fileSystem.files.entrySet()) {
                String path = entry.getKey();
                TestCase.File file = entry.getValue();

                if (file.contents != null) {
                    result.put(path, file.contents);
                }
                else {
                    if (file.fields == null) {
                        throw new IOException("File must be either string or json object of fields");
                    }
                    if (!file.fields.containsKey("project-path")) {
                        throw new IOException("File as object must have \"project-path\" field");
                    }

                    Path dir = Path.of(path.replace("${HASH}", projectPathHash(file.fields.get("project-path"))));

                    for (Entry<String, String> field : file.fields.entrySet()) {
                        result.put(combinePaths(dir, field.getKey()).toString(), field.getValue());
                    }
                }
            }

            return result;
        }

        private String projectPathHash(@NotNull String path) {
            if (isOSPlatform(OSType.WINDOWS) && !path.startsWith("\\\\")) {
                path = "\\\\?\\" + path;
            }

            return HexUtils.byteArrayToHexString(SHA1.digest(
                path.getBytes(StandardCharsets.UTF_8)
            ));
        }

        @Override
        public @NotNull Path getHomeDir() {
            if (homeDir != null) {
                return homeDir;
            }

            return super.getHomeDir();
        }

        @Override
        public @NotNull Path getCurrentDirectory() {
            if (homeDir != null) {
                return homeDir;
            }

            return super.getCurrentDirectory();
        }

        @Override
        public @Nullable String getEnvVariable(@NotNull String name) {
            if (envVars != null) {
                return envVars.getOrDefault(name, null);
            }
            return super.getEnvVariable(name);
        }

        @Override
        public boolean fileExists(@NotNull Path path) {
            return files.containsKey(path);
        }

        @Override
        public @Nullable String fileReadAllText(Path path) {
            return files.get(path);
        }

        @Override
        public void writeWarning(@NotNull String message) {
            warnings.add(message);
        }
    }

    //#endregion

    //#region TestCase

    public static class TestCase {

        public static final JsonMapper mapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

        @JsonProperty("name")
        public @NotNull String name = "";

        @JsonProperty("opts")
        public @Nullable OptionsData options;
        
        @JsonProperty("env")
        public @Nullable HashMap<String, String> envVars;

        @JsonProperty("platform")
        public @Nullable String platform;

        @JsonProperty("fs")
        public @Nullable FileSystemData fileSystem;

        @JsonProperty("warnings")
        public @Nullable List<String> warnings;

        @JsonProperty("result")
        public @Nullable ExpectedResult result;

        @JsonProperty("error")
        public @Nullable ExpectedError error;

        public static class OptionsData {
            @JsonProperty("instance")
            public @Nullable String instance;

            @JsonProperty("dsn")
            public @Nullable String dsn;

            @JsonProperty("credentials")
            public @Nullable String credentials;

            @JsonProperty("credentialsFile")
            public @Nullable String credentialsFile;

            @JsonProperty("host")
            public @Nullable String host;

            @JsonProperty("port")
            @JsonDeserialize(using = AsStringDeserializer.class)
            public @Nullable String port;

            @JsonProperty("database")
            public @Nullable String database;

            @JsonProperty("branch")
            public @Nullable String branch;

            @JsonProperty("user")
            public @Nullable String user;

            @JsonProperty("password")
            public @Nullable String password;

            @JsonProperty("secretKey")
            public @Nullable String secretKey;

            @JsonProperty("tlsCA")
            public @Nullable String tlsCA;

            @JsonProperty("tlsCAFile")
            public @Nullable String tlsCAFile;

            @JsonProperty("tlsSecurity")
            public @Nullable String tlsSecurity;

            @JsonProperty("tlsServerName")
            public @Nullable String tlsServerName;

            @JsonProperty("waitUntilAvailable")
            public @Nullable String waitUntilAvailable;

            @JsonProperty("serverSettings")
            public @Nullable HashMap<String, String> serverSettings;
        }

        public static class FileSystemData {
            @JsonProperty("cwd")
            public @Nullable String currentDir;

            @JsonProperty("homedir")
            public @Nullable String homeDir;

            @JsonProperty("files")
            public @Nullable HashMap<String, File> files;
        }

        @JsonDeserialize(using = FileDeserializer.class)
        public static class File {
            // Has either string contents or has explicitly defined instance information

            // string contents
            public @Nullable String contents;

            // instance information
            public @Nullable HashMap<String, String> fields;
        }

        public static class ExpectedResult {
            @JsonProperty("address")
            @JsonDeserialize(using = AsListStringDeserializer.class)
            public List<String> address;

            @JsonProperty("database")
            public @NotNull String database = "";

            @JsonProperty("branch")
            public @NotNull String branch = "";

            @JsonProperty("user")
            public @NotNull String user = "";

            @JsonProperty("password")
            public @Nullable String password;

            @JsonProperty("secretKey")
            public @Nullable String secretKey;

            @JsonProperty("tlsCAData")
            public @Nullable String tlsCAData;

            @JsonProperty("tlsSecurity")
            public @Nullable String tlsSecurity;

            @JsonProperty("tlsServerName")
            public @Nullable String tlsServerName;

            @JsonProperty("waitUntilAvailable")
            public @Nullable String waitUntilAvailable;

            @JsonProperty("serverSettings")
            public @Nullable HashMap<String, String> serverSettings;
        }

        public static class ExpectedError {
            @JsonProperty("type")
            public @NotNull String type = "";
        }

        public static class AsListStringDeserializer extends StdDeserializer<List<String>> {
            public AsListStringDeserializer() {
                this(null);
            }

            public AsListStringDeserializer(final Class<?> vc) {
                super(vc);
            }

            @Override
            public List<String> deserialize(
                JsonParser jsonParser,
                DeserializationContext deserializationContext
            ) throws IOException {
                List<String> result = new ArrayList<String>();
    
                while (true) {
                    JsonToken token = jsonParser.nextToken();
                    if (token.equals(JsonToken.END_ARRAY)) {
                        break;
                    }

                    if (token == JsonToken.VALUE_NUMBER_INT
                        || token == JsonToken.VALUE_NUMBER_FLOAT
                        || token == JsonToken.VALUE_STRING
                    ) {
                        result.add(jsonParser.getText());
                    }
                    else {
                        throw new IOException(String.format(
                            "Invalid %s token: \"%s\", expected NUMBER or STRING.",
                            token,
                            jsonParser.getText()
                        ));
                    }
                }

                return result;
            }
        }

        public static class FileDeserializer extends StdDeserializer<File> {
            public FileDeserializer() {
                this(null);
            }

            public FileDeserializer(final Class<?> vc) {
                super(vc);
            }

            @Override
            public File deserialize(
                JsonParser jsonParser,
                DeserializationContext deserializationContext
            ) throws IOException {
                if (jsonParser.currentToken().equals(JsonToken.VALUE_STRING)) {
                    // string contents
                    File result = new File();
                    result.contents = jsonParser.getText();
                    return result;
                }
                else if (jsonParser.currentToken().equals(JsonToken.START_OBJECT)) {
                    // instance information
                    HashMap<String, String> fields = new HashMap<String, String>();

                    while (true) {
                        JsonToken token = jsonParser.nextToken();
                        if (token.equals(JsonToken.END_OBJECT)) {
                            break;
                        }

                        if (!token.equals(JsonToken.FIELD_NAME)) {
                            throw new IOException(String.format(
                                "Invalid %s token: \"%s\", expected FIELD_NAME.",
                                token,
                                jsonParser.getText()
                            ));
                        }

                        String propertyName = jsonParser.getText();

                        token = jsonParser.nextToken();
                        if (!token.equals(JsonToken.VALUE_STRING)) {
                            throw new IOException(String.format(
                                "Invalid %s token: \"%s\", expected STRING.",
                                token,
                                jsonParser.getText()
                            ));
                        }

                        String value = jsonParser.getText();

                        fields.put(propertyName, value);
                    }

                    File result = new File();
                    result.fields = fields;
                    return result;
                }
                else {
                    throw new IOException("Could not read File object.");
                }
            }
        } 
    }

    //#endregion
}
