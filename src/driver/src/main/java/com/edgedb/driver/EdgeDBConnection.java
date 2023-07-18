package com.edgedb.driver;

import com.edgedb.driver.exceptions.ConfigurationException;
import com.edgedb.driver.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A class containing information on how to connect to a EdgeDB instance.
 */
@SuppressWarnings("CloneableClassWithoutClone")
public class EdgeDBConnection implements Cloneable {

    /**
     * Gets a {@linkplain Builder} used to construct a new {@linkplain EdgeDBConnection}.
     * @return A new builder instance.
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    private static final String EDGEDB_INSTANCE_ENV_NAME = "EDGEDB_INSTANCE";
    private static final String EDGEDB_DSN_ENV_NAME = "EDGEDB_DSN";
    private static final String EDGEDB_CREDENTIALS_FILE_ENV_NAME = "EDGEDB_CREDENTIALS_FILE";
    private static final String EDGEDB_USER_ENV_NAME = "EDGEDB_USER";
    private static final String EDGEDB_PASSWORD_ENV_NAME = "EDGEDB_PASSWORD";
    private static final String EDGEDB_DATABASE_ENV_NAME = "EDGEDB_DATABASE";
    private static final String EDGEDB_HOST_ENV_NAME = "EDGEDB_HOST";
    private static final String EDGEDB_PORT_ENV_NAME = "EDGEDB_PORT";

    private static final String EDGEDB_CLOUD_PROFILE_ENV_NAME = "EDGEDB_CLOUD_PROFILE";
    private static final String EDGEDB_SECRET_KEY_ENV_NAME = "EDGEDB_SECRET_KEY";
    private static final int DOMAIN_NAME_MAX_LEN = 62;

    private static final Pattern DSN_FORMATTER = Pattern.compile("^([a-z]+)://");
    private static final Pattern DSN_QUERY_PARAMETERS = Pattern.compile("((?:.(?!\\?))+$)");
    private static final Pattern DSN_FILE_ARG = Pattern.compile("(.*?)_file");
    private static final Pattern DSN_ENV_ARG = Pattern.compile("(.*?)_env");

    private static final JsonMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    /**
     * Constructs a new {@linkplain EdgeDBConnection}.
     * @param user The connections' user.
     * @param password The connections' password.
     * @param database The connections' database name.
     * @param hostname The connections' hostname.
     * @param port The connections' port.
     * @param tlsca The connections' tls certificate authority.
     * @param tlsSecurity The connections' tls security policy.
     */
    public EdgeDBConnection(
            String user, String password, String database,
            String hostname, Integer port, String tlsca,
            @Nullable TLSSecurityMode tlsSecurity
    ) {
        this.user = user;
        this.password = password;
        this.database = database;
        this.hostname = hostname;
        this.port = port;
        this.tlsca = tlsca;
        this.tlsSecurity = tlsSecurity;
    }

    /**
     * Constructs an empty {@linkplain EdgeDBConnection}
     */
    public EdgeDBConnection() { }

    @JsonProperty("user")
    private String user;

    @JsonProperty("password")
    private String password;

    @JsonProperty("database")
    private String database;

    @JsonIgnore
    private String hostname;

    @JsonProperty("port")
    private Integer port;

    @JsonProperty("tls_ca")
    private String tlsca;

    @JsonProperty("tls_security")
    private @Nullable TLSSecurityMode tlsSecurity;

    @JsonIgnore
    private @Nullable String secretKey;

    @JsonIgnore
    private @Nullable String cloudProfile;

    /**
     * Gets the current connections' username field.
     * @return The username part of the connection.
     */
    public @NotNull String getUsername() {
        return user == null ? "edgedb" : user;
    }

    /**
     * Sets the current connections username field
     * @param value The new username.
     */
    protected void setUsername(String value) {
        user = value;
    }

    /**
     * Gets the current connections' password field.
     * @return The password part of the connection.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the current connections password field.
     * @param value The new password.
     */
    protected void setPassword(String value) {
        password = value;
    }

    /**
     * Gets the current connections' hostname field.
     * @return The hostname part of the connection.
     */
    public @NotNull String getHostname() {
        return hostname == null ? "localhost" : hostname;
    }

    /**
     * Sets the current connections hostname field.
     * @param value The new hostname
     * @throws ConfigurationException The hostname is invalid
     */
    protected void setHostname(@NotNull String value) throws ConfigurationException {
        if (value.contains("/")) {
            throw new ConfigurationException("Cannot use UNIX socket for 'Hostname'");
        }

        if (value.contains(",")) {
            throw new ConfigurationException("DSN cannot contain more than one host");
        }

        hostname = value;
    }

    /**
     * Gets the current connections' port field.
     * @return The port of the connection.
     */
    public int getPort() {
        return port == null ? 5656 : port;
    }

    /**
     * Sets the current connections port field.
     * @param value The new port.
     */
    protected void setPort(int value) {
        port = value;
    }

    /**
     * Gets the current connections' database field.
     * @return The database part of the connection.
     */
    public @NotNull String getDatabase() {
        return database == null ? "edgedb" : database;
    }

    /**
     * Sets the current connections database field.
     * @param value The new database
     */
    protected void setDatabase(String value) {
        database = value;
    }

    /**
     * Gets the current connections' TLS certificate authority.
     * @return The TLS certificate authority of the connection.
     */
    public String getTLSCertificateAuthority() {
        return tlsca;
    }

    /**
     * Sets the current connections TLS certificate authority.
     * @param value The new TLS certificate authority.
     */
    protected void setTLSCertificateAuthority(String value) {
        tlsca = value;
    }

    /**
     * Gets the current connections' TLS security mode.
     * @return The TLS security mode of the connection.
     * @see TLSSecurityMode
     */
    public @NotNull TLSSecurityMode getTLSSecurity() {
        return tlsSecurity == null ? TLSSecurityMode.STRICT : this.tlsSecurity;
    }

    /**
     * Sets the current connections TLS security mode.
     * @param value The new TLS security mode.
     * @see TLSSecurityMode
     */
    protected void setTLSSecurity(TLSSecurityMode value) {
        tlsSecurity = value;
    }

    /**
     * Gets the secret key used to authenticate with cloud instances.
     * @return The secret key if present; otherwise {@code null}.
     */
    public @Nullable String getSecretKey() {
        return this.secretKey;
    }

    /**
     * Sets the secret key used to authenticate with cloud instances.
     * @param secretKey The secret key for cloud authentication.
     */
    protected void setSecretKey(@Nullable String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Gets the name of the cloud profile to use to resolve the secret key.
     * @return The cloud profile if present; otherwise {@code null}.
     */
    public @Nullable String getCloudProfile() {
        return this.cloudProfile == null ? "default" : this.cloudProfile;
    }

    /**
     * Sets the name of the cloud profile to use to resolve the secret key.
     * @param cloudProfile The name of the cloud profile.
     */
    protected void setCloudProfile(@Nullable String cloudProfile) {
        this.cloudProfile = cloudProfile;
    }

    /**
     * Creates a {@linkplain EdgeDBConnection} from a given DSN string.
     * @param dsn The DSN to create the connection from.
     * @return A {@linkplain EdgeDBConnection} representing the parameters within the provided DSN string.
     * @throws ConfigurationException The DSN is not properly formatted.
     * @throws IOException A file argument within the DSN cannot be found or be read.
     */
    public static @NotNull EdgeDBConnection fromDSN(@NotNull String dsn) throws ConfigurationException, IOException {
        if (!dsn.startsWith("edgedb://")) {
            throw new ConfigurationException(String.format("DSN schema 'edgedb' expected but got '%s'", dsn.split("://")[0]));
        }

        String database = null, username = null, port = null, host = null, password = null;

        Map<String, String> args = Collections.emptyMap();

        var formattedDSN = DSN_FORMATTER.matcher(dsn).replaceAll("");

        var queryParams = DSN_QUERY_PARAMETERS.matcher(dsn);

        if (queryParams.find()) {
            args = QueryParamUtils.splitQuery(queryParams.group(1).substring(1));

            // remove args from formatted dsn
            formattedDSN = formattedDSN.replace(queryParams.group(1), "");
        }

        var partA = formattedDSN.split("/");

        if (partA.length == 2) {
            database = partA[1];
            formattedDSN = partA[0];
        }

        var partB = formattedDSN.split("@");

        if (partB.length == 2 && !partB[1].equals("")) {
            if (partB[1].contains(","))
                throw new ConfigurationException("DSN cannot contain more than one host");

            var right = partB[1].split(":");

            if(right.length == 2) {
                host = right[0];
                port = right[1];
            }
            else {
                host = right[0];
            }

            var left = partB[0].split(":");

            if(left.length == 2) {
                username = left[0];
                password = left[1];
            }
            else {
                username = left[0];
            }
        }
        else if(!formattedDSN.endsWith("@")) {
            var sub = partB[0].split(":");

            if(sub.length == 2) {
                host = sub[0];
                port = sub[1];
            }
            else if(!StringsUtil.isNullOrEmpty(sub[0])) {
                host = sub[0];
            }
        }

        var connection = new EdgeDBConnection();

        if(database != null)
            connection.database = database;

        if(host != null)
            connection.setHostname(host);

        if(username != null)
            connection.user = username;

        if(password != null)
            connection.password = password;

        if(port != null) {
            try{
                connection.port = Integer.parseInt(port);
            }
            catch (NumberFormatException e) {
                throw new ConfigurationException("port was not in the correct format", e);
            }
        }

        for (var entry : args.entrySet()) {
            var fileMatch = DSN_FILE_ARG.matcher(entry.getKey());
            var envMatch = DSN_ENV_ARG.matcher(entry.getKey());

            String value;
            String key;

            if(fileMatch.matches()){
                key = fileMatch.group(1);

                var file = new File(entry.getValue());

                if(!file.exists()) {
                    throw new FileNotFoundException(String.format("The specified argument \"%s\"'s file was not found", key));
                }

                if(!file.isFile()) {
                    throw new IllegalArgumentException(String.format("The specified argument \"%s\" is not a file", key));
                }

                if(!file.canRead()) {
                    throw new IllegalArgumentException(String.format("The specified argument \"%s\"'s file cannot be read: missing permissions", key));
                }

                value = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }
            else if (envMatch.matches()) {
                key = entry.getKey();
                value = System.getenv(entry.getValue());

                if (value == null) {
                    throw new ConfigurationException(String.format("Environment variable \"%s\" couldn't be found", entry.getValue()));
                }
            }
            else  {
                key = entry.getKey();
                value = entry.getValue();
            }

            setArgument(connection, key, value);
        }

        return connection;
    }

    /**
     * Creates a {@linkplain EdgeDBConnection} from a project files path.
     * @param path The path to the {@code edgedb.toml} file
     * @return A {@linkplain EdgeDBConnection} that targets the instance hosting the project specified by the
     * {@code edgedb.toml} file.
     * @throws IOException The project file or one of its dependants doesn't exist.
     * @throws ConfigurationException A cloud instance parameter is invalid OR the instance name is in an invalid.
     */
    public static EdgeDBConnection fromProjectFile(@NotNull Path path) throws IOException, ConfigurationException {
        return fromProjectFile(path.toFile());
    }

    /**
     * Creates a {@linkplain EdgeDBConnection} from a project files path.
     * @param path The path to the {@code edgedb.toml} file
     * @return A {@linkplain EdgeDBConnection} that targets the instance hosting the project specified by the
     * {@code edgedb.toml} file.
     * @throws IOException The project file or one of its dependants doesn't exist.
     * @throws ConfigurationException A cloud instance parameter is invalid OR the instance name is in an invalid.
     */
    public static EdgeDBConnection fromProjectFile(@NotNull String path) throws IOException, ConfigurationException {
        return fromProjectFile(new File(path));
    }

    /**
     * Creates a {@linkplain EdgeDBConnection} from a project file.
     * @param file The {@code edgedb.toml} file
     * @return A {@linkplain EdgeDBConnection} that targets the instance hosting the project specified by the
     * {@code edgedb.toml} file.
     * @throws IOException The project file or one of its dependants doesn't exist
     * @throws ConfigurationException A cloud instance parameter is invalid OR the instance name is in an invalid.
     */
    public static EdgeDBConnection fromProjectFile(@NotNull File file) throws IOException, ConfigurationException {
        if(!file.exists()) {
            throw new FileNotFoundException("Couldn't find the specified project file");
        }

        file = file.getAbsoluteFile();

        var dirName = file.getParent();

        var projectDir = Paths.get(ConfigUtils.getInstanceProjectDirectory(dirName));

        if(!Files.isDirectory(projectDir)) {
            throw new FileNotFoundException(String.format("Couldn't find project directory for %s: %s", file, projectDir));
        }

        var instanceDetails = ConfigUtils.tryResolveInstanceCloudProfile(projectDir);

        if(instanceDetails.isEmpty() || instanceDetails.get().getLinkedInstanceName() == null) {
            throw new FileNotFoundException("Could not find instance name under project directory " + projectDir);
        }

        var connection = fromInstanceName(instanceDetails.get().getLinkedInstanceName(), instanceDetails.get().getProfile());

        Optional<String> database = ConfigUtils.tryResolveProjectDatabase(projectDir);
        if(database.isPresent()) {
            connection.setDatabase(database.get());
        }

        return connection;
    }

    /**
     * Creates a new {@linkplain EdgeDBConnection} from an instance name.
     * @param instanceName The name of the instance.
     * @return A {@linkplain EdgeDBConnection} that targets the specified instance.
     * @throws IOException The instance could not be found or one of its configuration files cannot be read.
     * @throws ConfigurationException A cloud instance parameter is invalid OR the instance name is in an invalid.
     * format.
     */
    public static EdgeDBConnection fromInstanceName(String instanceName) throws IOException, ConfigurationException {
        return fromInstanceName(instanceName, null);
    }

    /**
     * Creates a new {@linkplain EdgeDBConnection} from an instance name.
     * @param instanceName The name of the instance.
     * @param cloudProfile The optional cloud profile name if the instance is a cloud instance.
     * @return A {@linkplain EdgeDBConnection} that targets the specified instance.
     * @throws IOException The instance could not be found or one of its configuration files cannot be read.
     * @throws ConfigurationException A cloud instance parameter is invalid OR the instance name is in an invalid.
     * format.
     */
    public static EdgeDBConnection fromInstanceName(String instanceName, @Nullable String cloudProfile) throws IOException, ConfigurationException {
        if(Pattern.matches("^\\w(-?\\w)*$", instanceName)) {
            var configPath = Paths.get(ConfigUtils.getCredentialsDir(), instanceName + ".json");

            if(!Files.exists(configPath))
                throw new FileNotFoundException("Config file couldn't be found at " + configPath);

            return fromJSON(Files.readString(configPath, StandardCharsets.UTF_8));
        } else if (Pattern.matches("^([A-Za-z0-9](-?[A-Za-z0-9])*)/([A-Za-z0-9](-?[A-Za-z0-9])*)$", instanceName)) {
            var connection = new EdgeDBConnection();
            connection.parseCloudInstanceName(instanceName, cloudProfile);
            return connection;
        } else {
            throw new ConfigurationException(
                    String.format("Invalid instance name '%s'", instanceName)
            );
        }
    }

    /**
     * Resolves a connection by traversing the current working directory and its parents to find a {@code edgedb.toml}
     * file to use to create the {@linkplain EdgeDBConnection}.
     * @return A resolved {@linkplain EdgeDBConnection}.
     * @throws IOException No {@code edgedb.toml} file could be found, or one of its configuration files cannot be read.
     * @throws ConfigurationException A cloud instance parameter is invalid OR the instance name is in an invalid
     */
    public static EdgeDBConnection resolveEdgeDBTOML() throws IOException, ConfigurationException {
        var dir = Paths.get(System.getProperty("user.dir"));

        while(true) {
            var target = dir.resolve("edgedb.toml");

            if (Files.exists(target)) {
                return fromProjectFile(target);
            }

            var parent = dir.getParent();

            if(parent == null || !Files.exists(parent)) {
                throw new FileNotFoundException("Couldn't resolve edgedb.toml file");
            }

            dir = parent;
        }
    }

    private void parseCloudInstanceName(String name, @Nullable String cloudProfile) throws ConfigurationException, IOException {
        if(name.length() > DOMAIN_NAME_MAX_LEN) {
            throw new ConfigurationException(
                    String.format(
                            "Cloud instance name must be %d characters or less in length",
                            DOMAIN_NAME_MAX_LEN
                    )
            );
        }

        var secretKey = this.secretKey;

        if(secretKey == null) {
            if(cloudProfile == null) {
                cloudProfile = getCloudProfile();
            }

            var profile = ConfigUtils.readCloudProfile(cloudProfile, mapper);

            if(profile.secretKey == null) {
                throw new ConfigurationException(
                        String.format("Secret key in cloud profile '%s' cannot be null", cloudProfile)
                );
            }

            secretKey = profile.secretKey;
        }

        var spl = secretKey.split("\\.");

        if(spl.length < 2) {
            throw new ConfigurationException("Invalid secret key: doesn't contain payload");
        }

        TypeReference<HashMap<String, String>> typeRef = new TypeReference<>() {};

        var json = Base64.getDecoder().decode(spl[1]);
        var jsonData = mapper.readValue(json, typeRef);

        if(!jsonData.containsKey("iss")) {
            throw new ConfigurationException(
                    "Invalid secret key: payload doesn't contain 'iss' value"
            );
        }

        name = name.toLowerCase(Locale.ROOT);

        var dnsBucket = StringsUtil.padLeft(
                Integer.toString((CRCHQX.CRCHqx(name.getBytes(StandardCharsets.UTF_8), 0) % 100)),
                '0',
                2
        );

        spl = name.split("/");

        setHostname(
                String.format(
                        "%s--%s.c-%s.i.%s",
                        spl[1],
                        spl[0],
                        dnsBucket,
                        jsonData.get("iss")
        ));

        if(this.secretKey == null) {
            setSecretKey(secretKey);
        }
    }

    /**
     * Parses a connection from disc, and/or the connection argument as a DSN or instance name, then applying
     * environment variables to the connection.
     * @param connection The connection argument, usually a DSN or instance name.
     * @return A parsed {@linkplain EdgeDBConnection}.
     * @throws ConfigurationException One of the arguments is invalid.
     * @throws IOException One on the file arguments doesn't exist or cannot be read.
     */
    public static EdgeDBConnection parse(String connection) throws ConfigurationException, IOException {
        return parse(connection, null, true);
    }

    /**
     * Parses a connection from disc and/or environment variables, then applies the specified delegate to the
     * connection.
     * @param configure The delegate used to configure the resolved connection.
     * @return A parsed {@linkplain EdgeDBConnection}.
     * @throws ConfigurationException One of the arguments is invalid.
     * @throws IOException One on the file arguments doesn't exist or cannot be read.
     */
    public static EdgeDBConnection parse(
            ConfigureFunction configure
    ) throws ConfigurationException, IOException {
        return parse(null, configure, true);
    }

    /**
     * Parses a connection from disc, and/or the connection argument as a DSN or instance name, applying
     * environment variables to the connection, then calling the specified delegate with the parsed connection.
     * @param connection The connection argument, usually a DSN or instance name.
     * @param configure The delegate used to configure the resolved connection.
     * @return A parsed {@linkplain EdgeDBConnection}.
     * @throws ConfigurationException One of the arguments is invalid.
     * @throws IOException One on the file arguments doesn't exist or cannot be read.
     */
    public static EdgeDBConnection parse(
            String connection,
            ConfigureFunction configure
    ) throws ConfigurationException, IOException {
        return parse(connection, configure, true);
    }

    /**
     * Parses a connection from disc, and/or the connection argument as a DSN or instance name, applying
     * environment variables to the connection, then calling the specified delegate with the parsed connection.
     * @param connParam The connection argument, usually a DSN or instance name.
     * @param configure The delegate used to configure the resolved connection.
     * @param autoResolve Whether to resolve the connection from disc.
     * @return A parsed {@linkplain EdgeDBConnection}.
     * @throws ConfigurationException One of the arguments is invalid.
     * @throws IOException One on the file arguments doesn't exist or cannot be read.
     */
    public static EdgeDBConnection parse(
            @Nullable String connParam,
            @Nullable ConfigureFunction configure,
            boolean autoResolve
    ) throws ConfigurationException, IOException {
        return parse(connParam, configure, autoResolve, System::getenv);
    }

    /**
     * Parses a connection from disc, and/or the connection argument as a DSN or instance name, applying
     * environment variables to the connection, then calling the specified delegate with the parsed connection.
     * @param connParam The connection argument, usually a DSN or instance name.
     * @param configure The delegate used to configure the resolved connection.
     * @param autoResolve Whether to resolve the connection from disc.
     * @param resolveEnv A function to fetch environment variables.
     * @return A parsed {@linkplain EdgeDBConnection}.
     * @throws ConfigurationException One of the arguments is invalid.
     * @throws IOException One on the file arguments doesn't exist or cannot be read.
     */
    public static EdgeDBConnection parse(
            @Nullable String connParam,
            @Nullable ConfigureFunction configure,
            boolean autoResolve,
            @NotNull Function<String, String> resolveEnv
    ) throws ConfigurationException, IOException {
        var connection = new EdgeDBConnection();

        boolean isDSN = false;

        if(autoResolve && !((connParam != null && connParam.contains("/")) || (connParam != null && !connParam.startsWith("edgedb://")))) {
            try {
                connection = connection.mergeInto(resolveEdgeDBTOML());
            } catch (IOException x) {
                // ignore
            }
        }

        connection = applyEnv(connection, resolveEnv);

        if(connParam != null) {
            if(connParam.contains("://")) {
                connection = connection.mergeInto(fromDSN(connParam));
                isDSN = true;
            } else {
                connection = connection.mergeInto(fromInstanceName(connParam));
            }
        }

        if(configure != null) {
            var builder = builder();

            configure.accept(builder);

            if(isDSN && builder.hostname != null) {
                throw new ConfigurationException("Cannot specify DSN and 'Hostname'; they are mutually exclusive");
            }

            connection = connection.mergeInto(builder.build());
        }

        return connection;
    }

    private static EdgeDBConnection applyEnv(EdgeDBConnection connection, @NotNull Function<String, String> getEnv) throws ConfigurationException, IOException {
        var instanceName = getEnv.apply(EDGEDB_INSTANCE_ENV_NAME);
        var dsn = getEnv.apply(EDGEDB_DSN_ENV_NAME);
        var host = getEnv.apply(EDGEDB_HOST_ENV_NAME);
        var port = getEnv.apply(EDGEDB_PORT_ENV_NAME);
        var credentials = getEnv.apply(EDGEDB_CREDENTIALS_FILE_ENV_NAME);
        var user = getEnv.apply(EDGEDB_USER_ENV_NAME);
        var pass = getEnv.apply(EDGEDB_PASSWORD_ENV_NAME);
        var db = getEnv.apply(EDGEDB_DATABASE_ENV_NAME);
        var cloudProfile = getEnv.apply(EDGEDB_CLOUD_PROFILE_ENV_NAME);
        var cloudSecret = getEnv.apply(EDGEDB_SECRET_KEY_ENV_NAME);

        if(cloudProfile != null) {
            connection = connection.mergeInto(new EdgeDBConnection(){{
                setCloudProfile(cloudProfile);
            }});
        }

        if(cloudSecret != null) {
            connection = connection.mergeInto(new EdgeDBConnection(){{
                setSecretKey(cloudSecret);
            }});
        }

        if(instanceName != null) {
            connection = connection.mergeInto(fromInstanceName(instanceName));
        }

        if(dsn != null) {
            if(Pattern.matches("^([A-Za-z0-9](-?[A-Za-z0-9])*)/([A-Za-z0-9](-?[A-Za-z0-9])*)$", dsn)) {
                connection.parseCloudInstanceName(dsn, null);
            } else {
                connection = connection.mergeInto(fromDSN(dsn));
            }
        }

        if(host != null) {
            try {
                connection.setHostname(host);
            }
            catch (ConfigurationException x) {
                if(x.getMessage().equals("DSN cannot contain more than one host")) {
                    throw new ConfigurationException("Environment variable 'EDGEDB_HOST' cannot contain more than one host", x);
                }

                throw x;
            }
        }

        if(port != null) {
            try {
                connection.port = Integer.parseInt(port);
            }
            catch (NumberFormatException x) {
                throw new ConfigurationException(
                        String.format(
                                "Expected integer for environment variable '%s' but got '%s'",
                                EDGEDB_PORT_ENV_NAME,
                                port
                        )
                );
            }
        }

        if(credentials != null) {
            var path = Path.of(credentials);
            if(!Files.exists(path)) {
                throw new ConfigurationException(
                        String.format(
                                "Could not find the file specified in '%s'",
                                EDGEDB_CREDENTIALS_FILE_ENV_NAME
                        )
                );
            }

            connection = connection.mergeInto(fromJSON(Files.readString(path, StandardCharsets.UTF_8)));
        }

        if(user != null) {
            connection.user = user;
        }

        if(pass != null) {
            connection.password = pass;
        }

        if(db != null) {
            connection.database = db;
        }

        return connection;
    }

    private @NotNull EdgeDBConnection mergeInto(@NotNull EdgeDBConnection other) {
        if(other.tlsSecurity == null) {
            other.tlsSecurity = this.tlsSecurity;
        }

        if(other.database == null) {
            other.database = this.database;
        }

        if(other.hostname == null) {
            other.hostname = this.hostname;
        }

        if(other.password == null) {
            other.password = this.password;
        }

        if(other.tlsca == null) {
            other.tlsca = this.tlsca;
        }

        if(other.port == null) {
            other.port = this.port;
        }

        if(other.user == null) {
            other.user = this.user;
        }

        return other;
    }

    private static EdgeDBConnection fromJSON(String json) throws JsonProcessingException {
        return mapper.readValue(json, EdgeDBConnection.class);
    }

    private static void setArgument(@NotNull EdgeDBConnection connection, @NotNull String name, @NotNull String value) throws ConfigurationException, IllegalArgumentException, IOException {
        if (StringsUtil.isNullOrEmpty(value))
            return;

        switch (name) {
            case "port":
                if (connection.port != null) {
                    throw new IllegalArgumentException("Port ambiguity mismatch");
                }

                try {
                    connection.port = Integer.parseInt(value);
                }
                catch (NumberFormatException e) {
                    throw new ConfigurationException("port was not in the correct format", e);
                }
                break;
            case "host":
                if (connection.hostname != null) {
                    throw new IllegalArgumentException("Host ambiguity mismatch");
                }

                connection.setHostname(value);
                break;
            case "user":
                if (connection.user != null) {
                    throw new IllegalArgumentException("User ambiguity mismatch");
                }

                connection.user = value;
                break;
            case "password":
                if (connection.password != null) {
                    throw new IllegalArgumentException("Password ambiguity mismatch");
                }

                connection.password = value;
                break;
            case "tls_cert_file":
                var file = new File(value);

                if(!file.exists()) {
                    throw new FileNotFoundException("The specified tls_cert_file file was not found");
                }

                if(!file.isFile()) {
                    throw new IllegalArgumentException("The specified tls_cert_file is not a file");
                }

                if(!file.canRead()) {
                    throw new IllegalArgumentException("The specified tls_cert_file cannot be read: missing permissions");
                }

                connection.tlsca = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                break;
            case "tls_security":
                var security = EnumsUtil.searchEnum(TLSSecurityMode.class, value);

                if(security == null) {
                    throw new IllegalArgumentException(String.format("\"%s\" must be a value of edgedb.driver.TLSSecurityMode", value));
                }

                connection.tlsSecurity = security;
                break;
            default:
                throw new IllegalArgumentException(String.format("Unexpected configuration option %s", name));
        }
    }

    /**
     * Turns this connection into a valid DSN string.
     * @return A DSN string representing the parameters within the current {@linkplain EdgeDBConnection}.
     */
    @Override
    public @NotNull String toString() {
        var sb = new StringBuilder("edgedb://");

        this.getUsername();
        sb.append(this.getUsername());

        if(this.getPassword() != null) {
            sb.append(":");
            sb.append(this.getPassword());
        }

        this.getHostname();
        sb.append("@");
        sb.append(this.getHostname());
        sb.append(":");
        sb.append(this.getPort());

        this.getDatabase();
        sb.append("/");
        sb.append(this.getDatabase());

        return sb.toString();
    }

    @FunctionalInterface
    public interface ConfigureFunction {
        void accept(EdgeDBConnection.Builder connection) throws ConfigurationException;
    }

    /**
     * A builder class used to construct a {@linkplain EdgeDBConnection}
     */
    public static final class Builder {
        private String user;
        private String password;
        private String database;
        private String hostname;
        private Integer port;
        private String tlsca;
        private @Nullable TLSSecurityMode tlsSecurity;

        /**
         * Sets the connections' username.
         * @param user The new username.
         * @return The current builder.
         */
        public @NotNull Builder withUser(String user) {
            this.user = user;
            return this;
        }

        /**
         * Sets the connections' password.
         * @param password The new password.
         * @return The current builder.
         */
        public @NotNull Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the connections' database.
         * @param database The new database for the connection.
         * @return The current builder.
         */
        public @NotNull Builder withDatabase(String database) {
            this.database = database;
            return this;
        }

        /**
         * Sets the connections' hostname
         * @param hostname The new hostname for the connection.
         * @return The current builder
         * @throws ConfigurationException The hostnames format is invalid.
         */
        public @NotNull Builder withHostname(@NotNull String hostname) throws ConfigurationException {
            if (hostname.contains("/")) {
                throw new ConfigurationException("Cannot use UNIX socket for 'Hostname'");
            }

            if (hostname.contains(",")) {
                throw new ConfigurationException("DSN cannot contain more than one host");
            }
            this.hostname = hostname;

            return this;
        }

        /**
         * Sets the connections' port.
         * @param port The new port for the connection.
         * @return The current builder.
         */
        public @NotNull Builder withPort(Integer port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the connections' tls certificate authority.
         * @param tlsca The new tls certificate authority.
         * @return The current builder.
         */
        public @NotNull Builder withTlsca(String tlsca) {
            this.tlsca = tlsca;
            return this;
        }

        /**
         * Sets the connections' tls security policy.
         * @param tlsSecurity The new tls security policy.
         * @return The current builder.
         */
        public @NotNull Builder withTlsSecurity(TLSSecurityMode tlsSecurity) {
            this.tlsSecurity = tlsSecurity;
            return this;
        }

        /**
         * Constructs a {@linkplain EdgeDBConnection} from the current builder.
         * @return A {@linkplain EdgeDBConnection} that represents the current builder.
         */
        public @NotNull EdgeDBConnection build() {
            return new EdgeDBConnection(
                    this.user, this.password,
                    this.database, this.hostname,
                    this.port, this.tlsca,
                    this.tlsSecurity
            );
        }
    }
}
