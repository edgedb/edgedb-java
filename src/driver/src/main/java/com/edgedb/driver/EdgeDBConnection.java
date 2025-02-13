package com.edgedb.driver;

import com.edgedb.driver.abstractions.SystemProvider;
import com.edgedb.driver.exceptions.ConfigurationException;
import com.edgedb.driver.util.*;
import com.edgedb.driver.util.ConfigUtils.DatabaseOrBranch;
import com.edgedb.driver.util.ConfigUtils.ResolvedField;
import com.edgedb.driver.util.ConfigUtils.ResolvedFields;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class containing information on how to connect to a EdgeDB instance.
 */
@SuppressWarnings("CloneableClassWithoutClone")
public class EdgeDBConnection implements Cloneable {

    /**
     * Gets a {@linkplain Builder} used to construct a new {@linkplain EdgeDBConnection}.
     *
     * @return A new builder instance.
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    private static final String INSTANCE_ENV_NAME = "INSTANCE";
    private static final String DSN_ENV_NAME = "DSN";
    private static final String CREDENTIALS_FILE_ENV_NAME = "CREDENTIALS_FILE";
    private static final String HOST_ENV_NAME = "HOST";
    private static final String PORT_ENV_NAME = "PORT";
    private static final String DATABASE_ENV_NAME = "DATABASE";
    private static final String BRANCH_ENV_NAME = "BRANCH";
    private static final String USER_ENV_NAME = "USER";
    private static final String PASSWORD_ENV_NAME = "PASSWORD";
    private static final String SECRET_KEY_ENV_NAME = "SECRET_KEY";
    private static final String TLS_CA_ENV_NAME = "TLS_CA";
    private static final String CLIENT_SECURITY_ENV_NAME = "CLIENT_SECURITY";
    private static final String CLIENT_TLS_SECURITY_ENV_NAME = "CLIENT_TLS_SECURITY";
    private static final String TLS_SERVER_NAME_ENV_NAME = "TLS_SERVER_NAME";
    private static final String WAIT_UNTIL_AVAILABLE_ENV_NAME = "WAIT_UNTIL_AVAILABLE";
    private static final String CLOUD_PROFILE_ENV_NAME = "CLOUD_PROFILE";

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
     *
     * @param user        The connections' user.
     * @param password    The connections' password.
     * @param database    The connections' database name.
     * @param hostname    The connections' hostname.
     * @param port        The connections' port.
     * @param tlsca       The connections' tls certificate authority.
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
        this.tlsCertificateAuthority = tlsca;
        this.tlsSecurity = tlsSecurity;
    }

    /**
     * Constructs an empty {@linkplain EdgeDBConnection}
     */
    public EdgeDBConnection() {
    }

    //#region Main connection args

    /**
     * Gets the current connections' hostname field.
     *
     * @return The hostname part of the connection.
     */
    public @NotNull String getHostname() {
        return hostname == null ? _defaultHostname : hostname;
    }

    /**
     * Sets the current connections hostname field.
     *
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

    @JsonIgnore
    private @Nullable String hostname;
    private static final String _defaultHostname = "localhost";

    /**
     * Gets the current connections' port field.
     *
     * @return The port of the connection.
     */
    public int getPort() {
        return port == null ? _defaultPort : port;
    }

    /**
     * Sets the current connections port field.
     *
     * @param value The new port.
     */
    protected void setPort(int value) {
        port = value;
    }

    @JsonProperty("port")
    private @Nullable Integer port;
    private static final int _defaultPort = 5656;

    /**
     * Gets the current connections' database field.
     *
     * @return The database part of the connection.
     */
    public @NotNull String getDatabase() {
        if (database != null)
            return database;

        if (branch != null)
            return branch;

        return _defaultDatabase;
    }

    /**
     * Sets the current connections database field.
     *
     * @param value The new database.
     */
    protected void setDatabase(String value) {
        database = value;
    }

    @JsonProperty("database")
    private @Nullable String database;
    private static final String _defaultDatabase = "edgedb";

    /**
     * Gets the current connections' branch field.
     *
     * @return The branch part of the connection.
     */
    public @NotNull String getBranch() {
        if (branch != null)
            return branch;

        if (database != null)
            return database;

        return _defaultBranch;
    }

    /**
     * Sets the current connections branch field.
     *
     * @param value The new branch.
     */
    protected void setBranch(String value) {
        branch = value;
    }

    @JsonProperty("branch")
    private @Nullable String branch;
    private static final String _defaultBranch = "__default__";

    /**
     * Gets the current connections' username field.
     *
     * @return The username part of the connection.
     */
    public @NotNull String getUsername() {
        return user == null ? _defaultUser : user;
    }

    /**
     * Sets the current connections username field
     *
     * @param value The new username.
     */
    protected void setUsername(String value) {
        user = value;
    }

    @JsonProperty("user")
    private @Nullable String user;
    private static final String _defaultUser = "edgedb";

    /**
     * Gets the current connections' password field.
     *
     * @return The password part of the connection.
     */
    public @Nullable String getPassword() {
        return password;
    }

    /**
     * Sets the current connections password field.
     *
     * @param value The new password.
     */
    protected void setPassword(String value) {
        password = value;
    }

    @JsonProperty("password")
    private @Nullable String password;

    /**
     * Gets the secret key used to authenticate with cloud instances.
     *
     * @return The secret key if present; otherwise {@code null}.
     */
    public @Nullable String getSecretKey() {
        return this.secretKey;
    }

    /**
     * Sets the secret key used to authenticate with cloud instances.
     *
     * @param secretKey The secret key for cloud authentication.
     */
    protected void setSecretKey(@Nullable String secretKey) {
        this.secretKey = secretKey;
    }

    @JsonIgnore
    private @Nullable String secretKey;

    /**
     * Gets the current connections' TLS certificate authority.
     *
     * @return The TLS certificate authority of the connection.
     */
    public @Nullable String getTLSCertificateAuthority() {
        return tlsCertificateAuthority;
    }

    /**
     * Sets the current connections TLS certificate authority.
     *
     * @param value The new TLS certificate authority.
     */
    protected void setTLSCertificateAuthority(String value) {
        tlsCertificateAuthority = value;
    }

    @JsonProperty("tls_ca")
    private @Nullable String tlsCertificateAuthority;

    /**
     * Gets the current connections' TLS security mode.
     *
     * @return The TLS security mode of the connection.
     * @see TLSSecurityMode
     */
    public @NotNull TLSSecurityMode getTLSSecurity() {
        return tlsSecurity == null
            ? _defaultTlsSecurity
            : tlsSecurity == TLSSecurityMode.DEFAULT
            ? _defaultTlsSecurity
            : this.tlsSecurity;
    }

    /**
     * Sets the current connections TLS security mode.
     *
     * @param value The new TLS security mode.
     * @see TLSSecurityMode
     */
    protected void setTLSSecurity(TLSSecurityMode value) {
        tlsSecurity = value;
    }

    @JsonProperty("tls_security")
    private @Nullable TLSSecurityMode tlsSecurity;
    private static final TLSSecurityMode _defaultTlsSecurity = TLSSecurityMode.STRICT;

    public static class WaitTime {
        public final @NotNull Long value;
        public final @NotNull TimeUnit unit;
        public WaitTime(@NotNull Long value, @NotNull TimeUnit unit) {
            this.value = value;
            this.unit = unit;
        }

        public @NotNull long valueInUnits(@NotNull TimeUnit unit) {
            return unit.convert(this.value, this.unit);
        }
    }

    /**
     * Gets the time a client will wait for a connection to be established with the server.
     *
     * @return The time to wait.
     */
    public @NotNull WaitTime getWaitUntilAvailable() {
        return waitUntilAvailable == null ? _defaultWaitUntilAvailable : waitUntilAvailable;
    }

    /**
     * Sets the time a client will wait for a connection to be established with the server.
     *
     * @param value The time to wait.
     */
    protected void setWaitUntilAvailable(WaitTime waitUntilAvailable) {
        this.waitUntilAvailable = waitUntilAvailable;
    }

    private @Nullable WaitTime waitUntilAvailable;
    private static final @NotNull WaitTime _defaultWaitUntilAvailable = new WaitTime(30l, TimeUnit.SECONDS);

    /**
     * Gets the name of the cloud profile to use to resolve the secret key.
     *
     * @return The cloud profile if present; otherwise {@code null}.
     */
    public @Nullable String getCloudProfile() {
        return this.cloudProfile == null ? "default" : this.cloudProfile;
    }

    /**
     * Sets the name of the cloud profile to use to resolve the secret key.
     *
     * @param cloudProfile The name of the cloud profile.
     */
    protected void setCloudProfile(@Nullable String cloudProfile) {
        this.cloudProfile = cloudProfile;
    }

    @JsonIgnore
    private @Nullable String cloudProfile;

    /**
     * Gets the TLS server name to be used
     * 
     * Overrides the value provided by Hostname.
     *
     * @return The TLS server name to be used if present; otherwise {@code null}.
     */
    public @Nullable String getTLSServerName() {
        return tlsServerName;
    }

    @JsonIgnore
    private @Nullable String tlsServerName;

    private @NotNull HashMap<String, String> serverSettings = new HashMap<String, String>();

    //#endregion

    private static EdgeDBConnection _fromResolvedFields(
        ConfigUtils.ResolvedFields resolvedFields,
        SystemProvider platform
    ) throws ConfigurationException {
        EdgeDBConnection result = new EdgeDBConnection();

        result.hostname = ConfigUtils.checkAndGetFieldValue(
            resolvedFields.host,
            (String value) -> {
                if (value == null) {
                    return;
                }

                if (value.contains(",")) {
                    throw new ConfigurationException(String.format(
                        "Invalid host: \"%s\", DSN cannot contain more than one host",
                        value
                    ));
                }
                if (value == "") {
                    throw new ConfigurationException(String.format(
                        "Invalid host: \"%s\"",
                        value
                    ));
                }
                if (value.startsWith("/")) {
                    throw new ConfigurationException(String.format(
                        "Invalid host: \"%s\", unix socket paths not supported",
                        value
                    ));
                }
            }
        );

        result.port = ConfigUtils.checkAndGetFieldValue(
            resolvedFields.port,
            (Integer value) -> {
                if (value == null) {
                    return;
                }

                if (value < 1 || 65535 < value) {
                    throw new ConfigurationException(String.format(
                        "Invalid port: \"%i\", must be between 1 and 65535",
                        value
                    ));
                }
            }
        );

        @Nullable DatabaseOrBranch databaseOrBranch = ConfigUtils.checkAndGetFieldValue(
            resolvedFields.databaseOrBranch,
            (DatabaseOrBranch value) -> {
                if (value == null) {
                    return;
                }

                if (value.getDatabase() != null && value.getDatabase().equals("")) {
                    throw new ConfigurationException(String.format(
                        "Invalid database name: \"%s\"",
                        value.getDatabase()
                    ));
                }
                if (value.getBranch() != null && value.getBranch().equals("")) {
                    throw new ConfigurationException(String.format(
                        "Invalid branch name: \"%s\"",
                        value.getDatabase()
                    ));
                }
            }
        );
        result.database = databaseOrBranch != null ? databaseOrBranch.getDatabase() : null;
        result.branch = databaseOrBranch != null ? databaseOrBranch.getBranch() : null;

        result.user = ConfigUtils.checkAndGetFieldValue(
            resolvedFields.user,
            (String value) -> {
                if (value == null) {
                    return;
                }

                if (value.equals("")) {
                    throw new ConfigurationException(String.format(
                        "Invalid user: \"%s\"",
                        value
                    ));
                }
            }
        );

        result.password = ConfigUtils.checkAndGetFieldValue(resolvedFields.password);
        result.secretKey = ConfigUtils.checkAndGetFieldValue(resolvedFields.secretKey);
        result.tlsCertificateAuthority = ConfigUtils.checkAndGetFieldValue(resolvedFields.tlsCertificateAuthority);
        result.tlsSecurity = ConfigUtils.checkAndGetFieldValue(resolvedFields.tlsSecurity);
        result.tlsServerName = ConfigUtils.checkAndGetFieldValue(resolvedFields.tlsServerName);
        result.waitUntilAvailable = ConfigUtils.checkAndGetFieldValue(resolvedFields.waitUntilAvailable);
        result.serverSettings = ConfigUtils.checkAndGetServerSettings(resolvedFields.serverSettings);

        return result;
    }

    static private final Pattern _dsnRegex = Pattern.compile(
        "^(?:(?:edgedb|gel|(?<invalidScheme>\\w+))://)"
        + "(?:"
            + "(?:"
                + "(?<user>[^@/?:,]+)(?::(?<password>[^@/?:,]+))?@"
                + "|(?<invalidUser>[^@/?]+)@"
                + ")?"
            + "(?:"
                + "(?:(?<host0>[^@/?:]+)|\\[(?<host1>[^\\[\\]]+)\\])"
                    + "(?::(?<port>[^@/?:,]+))?"
                + "|(?<invalidHost>[^@/?]+)"
                + ")"
            + ")?"
        + "(?:"
            + "/(?<branch>[^@/?:,]*(?:/[^@/?:,]+)*)"
            + "|/(?<invalidBranch>[^/?]+)"
            + ")?"
        + "(?:\\?(?<params>.*))?"
        + "$"
    );
    private static final Pattern _dsnParamsRegex = Pattern.compile(
        "^(?:[^=&]+(?:=[^=&]*)?)(?:&(?:[^=&]+(?:=[^=&]*)?))*$"
    );

    /**
     * Creates a {@linkplain EdgeDBConnection} from a given DSN string.
     *
     * @param dsn The DSN to create the connection from.
     * @return A {@linkplain EdgeDBConnection} representing the parameters within the provided DSN string.
     * @throws ConfigurationException The DSN is not properly formatted.
     * @throws IOException            A file argument within the DSN cannot be found or be read.
     */
    public static @NotNull EdgeDBConnection fromDSN(
        @NotNull String dsn
    ) throws ConfigurationException, IOException {

        return _fromResolvedFields(
            _fromDSN(dsn, ConfigUtils.getDefaultSystemProvider()),
            ConfigUtils.getDefaultSystemProvider()
        );
    }

    private static @NotNull ResolvedFields _fromDSN(
        @NotNull String dsn,
        @NotNull SystemProvider provider
    ) throws ConfigurationException {
        Matcher dsnMatcher = _dsnRegex.matcher(URLDecoder.decode(dsn, StandardCharsets.UTF_8));
        if (!dsnMatcher.find()) {
            throw new ConfigurationException(String.format("Invalid DSN: \"%s\"", dsn));
        }
        if (dsnMatcher.group("invalidScheme") != null) {
            String scheme = dsnMatcher.group("invalidScheme");
            throw new ConfigurationException(String.format(
                "Invalid DSN scheme. Expected \"gel\" but got \"%s\"",
                scheme
            ));
        }
        if (dsnMatcher.group("invalidUser") != null) {
            throw new ConfigurationException("Invalid DSN: Could not parse user/password");
        }
        if (dsnMatcher.group("invalidHost") != null) {
            throw new ConfigurationException("Invalid DSN: Could not parse host/port");
        }
        if (dsnMatcher.group("invalidBranch") != null) {
            throw new ConfigurationException("Invalid DSN: Could not parse branch");
        }

        @Nullable String username = dsnMatcher.group("user");
        @Nullable String password = dsnMatcher.group("password");
        @Nullable String port = dsnMatcher.group("port");
        @Nullable String host = dsnMatcher.group("host0");
        if (host == null) {
            host = dsnMatcher.group("host1");
        }
        @Nullable String branch = dsnMatcher.group("branch");

        // Check that a param != used twice in the dsn
        HashSet<String> usedParamNames = new HashSet<String>();
        if (branch != null && branch != "") usedParamNames.add("branch");
        if (host != null && host != "") usedParamNames.add("host");
        if (username != null && username != "") usedParamNames.add("user");
        if (password != null && password != "") usedParamNames.add("password");
        if (port != null && port != "") usedParamNames.add("port");

        // Parse query params
        HashMap<String, String> args = new HashMap<String, String>();
        if (dsnMatcher.group("params") != null) {

            if (!_dsnParamsRegex.matcher(dsnMatcher.group("params")).find()) {
                throw new ConfigurationException("Invalid DSN: could not parse query parameters");
            }

            String[] params = dsnMatcher.group("params").split("&");
            for (String param : params) {
                String[] entry = param.split("=");
                if (entry.length == 2) {
                    String paramName = entry[0].toLowerCase();
                    if (args.containsKey(paramName)) {
                        throw new ConfigurationException(String.format(
                            "Invalid DSN: dupliate query parameter \"%s\"",
                            entry[0]
                        ));
                    }

                    if (paramName.endsWith("_env")) {
                        paramName = paramName.substring(0, paramName.length() - "_env".length());
                    }
                    if (paramName.endsWith("_file")) {
                        paramName = paramName.substring(0, paramName.length() - "_file".length());
                    }

                    if (usedParamNames.contains(paramName)) {
                        throw new ConfigurationException(String.format(
                            "Invalid DSN: more than one of "
                            + "\"%s\", "
                            + "\"?%s=\", \"?%s_env=\", \"?%s_file=\" "
                            + "was specified.",
                            paramName,
                            paramName,
                            paramName,
                            paramName
                        ));
                    }

                    args.put(paramName, entry[1]);
                    usedParamNames.add(paramName);
                }
                else {
                    if (entry[0].equals("tls_security")) {
                        throw new ConfigurationException("Invalid TLS Security in dsn query parameters");
                    }
                    else {
                        throw new ConfigurationException(String.format(
                            "Invalid %s in dsn query parameters",
                            entry[0]
                        ));
                    }
                }
            }
        }

        var resolvedFields = new ConfigUtils.ResolvedFields();

        if (host != null) {
            resolvedFields.host = ResolvedField.valid(host);
        }
        if (port != null) {
            try {
                resolvedFields.port = ResolvedField.valid(Integer.parseInt(port));
            }
            catch (NumberFormatException e) {
                throw new ConfigurationException("Invalid DSN: port was not in the correct format");
            }
        }
        if (branch != null && !branch.equals("")) {
            resolvedFields.databaseOrBranch = ResolvedField.valid(DatabaseOrBranch.ofBranch(branch));
        }
        if (username != null) {
            resolvedFields.user = ResolvedField.valid(username);
        }
        if (password != null) {
            resolvedFields.password = ResolvedField.valid(password);
        }

        {
            boolean hasBranchArg = false;
            boolean hasDatabaseArg = false;
            for (String key : args.keySet()) {
                if (key.startsWith("branch")) {
                    hasBranchArg = true;
                }
                else if (key.startsWith("database")) {
                    hasDatabaseArg = true;
                }
            }
            if (hasBranchArg && hasDatabaseArg) {
                throw new ConfigurationException("Invalid DSN: branch and database are mutually exclusive");
            }
        }

        // Resolve query arguments
        for (Entry<String, String> arg : args.entrySet()) {
            String key = arg.getKey();
            ResolvedField<String> value = ResolvedField.valid(arg.getValue());

            if (key.endsWith("_env") && value.getValue() != null) {
                String envName = value.getValue();

                String oldKey = key;
                key = key.substring(0, key.length() - "_env".length());
                @Nullable String envVar = provider.getEnvVariable(value.getValue());
                if (envVar != null) {
                    value = ResolvedField.valid(envVar);
                }
                else {
                    value = ResolvedField.invalid(
                        new ConfigurationException(String.format(
                            "Invalid DSN query parameter: \"%s\", environment variable \"%s\" doesn\'t exist",
                            oldKey,
                            envName
                        )
                    ));
                }
            }

            if (key.endsWith("_file") && value.getValue() != null) {
                Path fileName = Path.of(value.getValue());

                String oldKey = key;
                key = key.substring(0, key.length() - "_file".length());

                String fileText = null;
                try {
                    if (provider.fileExists(fileName)) {
                        fileText = provider.fileReadAllText(fileName);
                    }
                }
                catch (IOException e) {}

                if (fileText != null) {
                    value = ResolvedField.valid(fileText);
                }
                else {
                    value = ResolvedField.invalid(
                        new ConfigurationException(String.format(
                            "Invalid DSN query parameter: \"%s\" could not find file \"%s\"",
                            oldKey,
                            fileName
                        ))
                    );
                }
            }

            if (value == null) { continue; }

            switch (key) {
                case "host":
                    resolvedFields.host = value;
                    break;
                case "port":
                    resolvedFields.port = value.convert(ConfigUtils::parsePort);
                    break;
                case "database":
                    resolvedFields.databaseOrBranch = value.convert((v) -> {
                        if (v.startsWith("/")) {
                            v = v.substring(1);
                        }
                        if (!v.equals("")) {
                            return ResolvedField.valid(DatabaseOrBranch.ofDatabase(v));
                        }
                        return null;
                    });
                    break;
                case "branch":
                    resolvedFields.databaseOrBranch = value.convert((v) -> {
                        if (v.startsWith("/")) {
                            v = v.substring(1);
                        }
                        if (!v.equals("")) {
                            return ResolvedField.valid(DatabaseOrBranch.ofBranch(v));
                        }
                        return null;
                    });
                    break;
                case "user":
                    resolvedFields.user = value;
                    break;
                case "password":
                    resolvedFields.password = value;
                    break;
                case "secret_key":
                    resolvedFields.secretKey = value;
                    break;
                case "tls_cert_file":
                    resolvedFields.tlsCertificateAuthority = value.convert((v) -> {
                        Path fileName = Path.of(v);

                        String fileText = null;
                        try {
                            if (provider.fileExists(fileName)) {
                                fileText = provider.fileReadAllText(fileName);
                            }
                        }
                        catch (IOException e) {}
        
                        if (fileText != null) {
                            return ResolvedField.valid(fileText);
                        }
                        else {
                            return ResolvedField.invalid(
                                new ConfigurationException("The specified tls_cert_file file was not found")
                            );
                        }
                    });
                    break;
                case "tls_server_name":
                    resolvedFields.tlsServerName = value;
                    break;
                case "tls_security":
                    resolvedFields.tlsSecurity = value.convert(ConfigUtils::parseTLSSecurityMode);
                    break;
                case "wait_until_available":
                    resolvedFields.waitUntilAvailable = value.convert(ConfigUtils::parseWaitUntilAvailable);
                    break;

                default:
                    resolvedFields.serverSettings = ConfigUtils.addServerSettingField(
                        resolvedFields.serverSettings,
                        key,
                        value
                    );
                    break;
            }
        }

        return resolvedFields;
    }

    /**
     * Creates a {@linkplain EdgeDBConnection} from a project files path.
     *
     * @param path The path to the {@code edgedb.toml} file
     * @return A {@linkplain EdgeDBConnection} that targets the instance hosting the project specified by the
     * {@code edgedb.toml} file.
     * @throws IOException            The project file or one of its dependants doesn't exist.
     * @throws ConfigurationException A cloud instance parameter is invalid OR the instance name is in an invalid.
     */
    public static @NotNull EdgeDBConnection fromProjectFile(
        @NotNull Path path
    ) throws IOException, ConfigurationException {
        return _fromProjectFile(path, ConfigUtils.getDefaultSystemProvider());
    }

    /**
     * Creates a {@linkplain EdgeDBConnection} from a project file.
     *
     * @param path The {@code edgedb.toml} file
     * @return A {@linkplain EdgeDBConnection} that targets the instance hosting the project specified by the
     * {@code edgedb.toml} file.
     * @throws IOException            The project file or one of its dependants doesn't exist
     * @throws ConfigurationException A cloud instance parameter is invalid OR the instance name is in an invalid.
     */
    private static @NotNull EdgeDBConnection _fromProjectFile(
        @NotNull Path path,
        @NotNull SystemProvider provider
    ) throws IOException, ConfigurationException {

        if (!provider.fileExists(path)) {
            throw new FileNotFoundException("Couldn't find the specified project file");
        }

        path = provider.getFullPath(path);

        Path dirName = path.getParent();

        Path projectDir = ConfigUtils.getInstanceProjectDirectory(dirName, provider);

        if (!provider.directoryExists(projectDir)) {
            throw new FileNotFoundException(String.format("Couldn't find project directory for %s: %s", path, projectDir));
        }

        ConfigUtils.CloudInstanceDetails instanceDetails =
            ConfigUtils.tryResolveInstanceCloudProfile(projectDir, provider);

        if (instanceDetails == null || instanceDetails.getLinkedInstanceName() == null) {
            throw new FileNotFoundException("Could not find instance name under project directory " + projectDir);
        }

        EdgeDBConnection connection = _fromInstanceName(
            instanceDetails.getLinkedInstanceName(),
            instanceDetails.getProfile(),
            null,
            provider
        );

        String database = ConfigUtils.tryResolveProjectDatabase(projectDir, provider);
        if (database != null) {
            connection.setDatabase(database);
        }

        return connection;
    }

    /**
     * Creates a new {@linkplain EdgeDBConnection} from an instance name.
     *
     * @param instanceName The name of the instance.
     * @return A {@linkplain EdgeDBConnection} that targets the specified instance.
     * @throws IOException            The instance could not be found or one of its configuration files cannot be read.
     * @throws ConfigurationException A cloud instance parameter is invalid OR the instance name is in an invalid.
     *                                format.
     */
    public static @NotNull EdgeDBConnection fromInstanceName(
        String instanceName
    ) throws IOException, ConfigurationException {

        return _fromInstanceName(
            instanceName,
            null,
            null, 
            ConfigUtils.getDefaultSystemProvider()
        );
    }

    /**
     * Creates a new {@linkplain EdgeDBConnection} from an instance name.
     *
     * @param instanceName The name of the instance.
     * @param cloudProfile The optional cloud profile name if the instance is a cloud instance.
     * @return A {@linkplain EdgeDBConnection} that targets the specified instance.
     * @throws IOException            The instance could not be found or one of its configuration files cannot be read.
     * @throws ConfigurationException A cloud instance parameter is invalid OR the instance name is in an invalid.
     *                                format.
     */
    public static @NotNull EdgeDBConnection fromInstanceName(
        String instanceName,
        @Nullable String cloudProfile
    ) throws IOException, ConfigurationException {

        return _fromInstanceName(
            instanceName,
            cloudProfile,
            null,
            ConfigUtils.getDefaultSystemProvider()
        );
    }

    private static @NotNull EdgeDBConnection _fromInstanceName(
        String instanceName,
        @Nullable String cloudProfile,
        @Nullable EdgeDBConnection connection,
        @NotNull SystemProvider provider
    ) throws IOException, ConfigurationException {

        if (Pattern.matches("^\\w(-?\\w)*$", instanceName)) {
            var configPath = provider.combinePaths(ConfigUtils.getCredentialsDir(provider), instanceName + ".json");

            if (!provider.fileExists(configPath))
                throw new FileNotFoundException("Config file couldn't be found at " + configPath);

            return fromJSON(provider.fileReadAllText(configPath));
        } else if (Pattern.matches("^([A-Za-z0-9](-?[A-Za-z0-9])*)/([A-Za-z0-9](-?[A-Za-z0-9])*)$", instanceName)) {
            if (connection == null) {
                connection = new EdgeDBConnection();
            }
            connection.parseCloudInstanceName(instanceName, cloudProfile, provider);
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
     *
     * @return A resolved {@linkplain EdgeDBConnection}.
     * @throws IOException            No {@code edgedb.toml} file could be found, or one of its configuration files cannot be read.
     * @throws ConfigurationException A cloud instance parameter is invalid OR the instance name is in an invalid
     */
    public static @NotNull EdgeDBConnection resolveEdgeDBTOML() throws IOException, ConfigurationException {
        return _resolveEdgeDBTOML(ConfigUtils.getDefaultSystemProvider());
    }

    private static @NotNull EdgeDBConnection _resolveEdgeDBTOML(
        @NotNull SystemProvider provider
    ) throws IOException, ConfigurationException {
        var dir = provider.getCurrentDirectory();

        while (true) {
            if (provider.fileExists(provider.combinePaths(dir, "edgedb.toml"))) {
                return _fromProjectFile(provider.combinePaths(dir, "edgedb.toml"), provider);
            }

            if (provider.fileExists(provider.combinePaths(dir, "gel.toml"))) {
                return _fromProjectFile(provider.combinePaths(dir, "gel.toml"), provider);
            }

            var parent = dir.getParent();

            if (parent == null || !provider.directoryExists(parent)) {
                throw new FileNotFoundException("Couldn't resolve edgedb.toml file");
            }

            dir = parent;
        }
    }

    private void parseCloudInstanceName(
        @NotNull String name,
        @Nullable String cloudProfile,
        @NotNull SystemProvider provider
    ) throws ConfigurationException, IOException {

        if (name.length() > DOMAIN_NAME_MAX_LEN) {
            throw new ConfigurationException(
                String.format(
                    "Cloud instance name must be %d characters or less in length",
                    DOMAIN_NAME_MAX_LEN
                )
            );
        }

        var secretKey = this.secretKey;

        if (secretKey == null) {
            if (cloudProfile == null) {
                cloudProfile = getCloudProfile();
            }

            var profile = ConfigUtils.readCloudProfile(cloudProfile, mapper, provider);

            if (profile.secretKey == null) {
                throw new ConfigurationException(
                        String.format("Secret key in cloud profile '%s' cannot be null", cloudProfile)
                );
            }

            secretKey = profile.secretKey;
        }

        var spl = secretKey.split("\\.");

        if (spl.length < 2) {
            throw new ConfigurationException("Invalid secret key: doesn't contain payload");
        }

        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};

        var json = Base64.getDecoder().decode(spl[1]);
        var jsonData = mapper.readValue(json, typeRef);

        if (!jsonData.containsKey("iss")) {
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

        if (this.secretKey == null) {
            setSecretKey(secretKey);
        }
    }

    /**
     * Parses a connection from disc, and/or the connection argument as a DSN or instance name, then applying
     * environment variables to the connection.
     *
     * @param connection The connection argument, usually a DSN or instance name.
     * @return A parsed {@linkplain EdgeDBConnection}.
     * @throws ConfigurationException One of the arguments is invalid.
     * @throws IOException            One on the file arguments doesn't exist or cannot be read.
     */
    public static @NotNull EdgeDBConnection parse(
        String connection
    ) throws ConfigurationException, IOException {

        return parse(connection, null, true);
    }

    /**
     * Parses a connection from disc and/or environment variables, then applies the specified delegate to the
     * connection.
     *
     * @param configure The delegate used to configure the resolved connection.
     * @return A parsed {@linkplain EdgeDBConnection}.
     * @throws ConfigurationException One of the arguments is invalid.
     * @throws IOException            One on the file arguments doesn't exist or cannot be read.
     */
    public static @NotNull EdgeDBConnection parse(
        ConfigureFunction configure
    ) throws ConfigurationException, IOException {

        return parse(null, configure, true);
    }

    /**
     * Parses a connection from disc, and/or the connection argument as a DSN or instance name, applying
     * environment variables to the connection, then calling the specified delegate with the parsed connection.
     *
     * @param connection The connection argument, usually a DSN or instance name.
     * @param configure  The delegate used to configure the resolved connection.
     * @return A parsed {@linkplain EdgeDBConnection}.
     * @throws ConfigurationException One of the arguments is invalid.
     * @throws IOException            One on the file arguments doesn't exist or cannot be read.
     */
    public static @NotNull EdgeDBConnection parse(
        String connection,
        ConfigureFunction configure
    ) throws ConfigurationException, IOException {

        return parse(connection, configure, true);
    }

    /**
     * Parses a connection from disc, and/or the connection argument as a DSN or instance name, applying
     * environment variables to the connection, then calling the specified delegate with the parsed connection.
     *
     * @param connParam   The connection argument, usually a DSN or instance name.
     * @param configure   The delegate used to configure the resolved connection.
     * @param autoResolve Whether to resolve the connection from disc.
     * @return A parsed {@linkplain EdgeDBConnection}.
     * @throws ConfigurationException One of the arguments is invalid.
     * @throws IOException            One on the file arguments doesn't exist or cannot be read.
     */
    public static @NotNull EdgeDBConnection parse(
        @Nullable String connParam,
        @Nullable ConfigureFunction configure,
        boolean autoResolve
    ) throws ConfigurationException, IOException {

        return _parse(connParam, configure, autoResolve, ConfigUtils.getDefaultSystemProvider());
    }

    static @NotNull EdgeDBConnection _parse(
        @Nullable String connParam,
        @Nullable ConfigureFunction configure,
        boolean autoResolve,
        @NotNull SystemProvider provider
    ) throws ConfigurationException, IOException {

        var connection = new EdgeDBConnection();

        boolean isDSN = false;

        if (autoResolve && !((connParam != null && connParam.contains("/")) || (connParam != null && !connParam.startsWith("edgedb://")))) {
            try {
                connection = connection.mergeInto(resolveEdgeDBTOML());
            } catch (IOException x) {
                // ignore
            }
        }

        connection = applyEnv(connection, provider);

        if (connParam != null) {
            if (connParam.contains("://")) {
                connection = connection.mergeInto(fromDSN(connParam));
                isDSN = true;
            } else {
                connection = connection.mergeInto(fromInstanceName(connParam));
            }
        }

        if (configure != null) {
            var builder = builder();

            configure.accept(builder);

            if (isDSN && builder.hostname != null) {
                throw new ConfigurationException("Cannot specify DSN and 'Hostname'; they are mutually exclusive");
            }

            connection = connection.mergeInto(builder.build());
        }

        return connection;
    }

    private static @NotNull EdgeDBConnection applyEnv(
        EdgeDBConnection connection,
        @NotNull SystemProvider provider
    ) throws ConfigurationException, IOException {

        var instanceName = SystemProvider.getGelEnvVariable(provider, INSTANCE_ENV_NAME);
        var dsn = SystemProvider.getGelEnvVariable(provider, DSN_ENV_NAME);
        var host = SystemProvider.getGelEnvVariable(provider, HOST_ENV_NAME);
        var port = SystemProvider.getGelEnvVariable(provider, PORT_ENV_NAME);
        var credentials = SystemProvider.getGelEnvVariable(provider, CREDENTIALS_FILE_ENV_NAME);
        var user = SystemProvider.getGelEnvVariable(provider, USER_ENV_NAME);
        var pass = SystemProvider.getGelEnvVariable(provider, PASSWORD_ENV_NAME);
        var db = SystemProvider.getGelEnvVariable(provider, DATABASE_ENV_NAME);
        var cloudProfile = SystemProvider.getGelEnvVariable(provider, CLOUD_PROFILE_ENV_NAME);
        var cloudSecret = SystemProvider.getGelEnvVariable(provider, SECRET_KEY_ENV_NAME);
        var branch = SystemProvider.getGelEnvVariable(provider, BRANCH_ENV_NAME);

        if (cloudProfile != null) {
            connection = connection.mergeInto(new EdgeDBConnection() {{
                setCloudProfile(cloudProfile.value);
            }});
        }

        if (cloudSecret != null) {
            connection = connection.mergeInto(new EdgeDBConnection() {{
                setSecretKey(cloudSecret.value);
            }});
        }

        if (instanceName != null) {
            connection = connection.mergeInto(
                _fromInstanceName(instanceName.value, null, connection, provider)
            );
        }

        if (dsn != null) {
            if (Pattern.matches(
                "^([A-Za-z0-9](-?[A-Za-z0-9])*)/([A-Za-z0-9](-?[A-Za-z0-9])*)$",
                dsn.value
            )) {
                connection.parseCloudInstanceName(dsn.value, null, provider);
            } else {
                connection = connection.mergeInto(fromDSN(dsn.value));
            }
        }

        if (host != null) {
            try {
                connection.setHostname(host.value);
            } catch (ConfigurationException x) {
                if (x.getMessage().equals("DSN cannot contain more than one host")) {
                    throw new ConfigurationException(
                        "Environment variable 'EDGEDB_HOST' cannot contain more than one host",
                        x
                    );
                }

                throw x;
            }
        }

        if (port != null) {
            try {
                connection.port = Integer.parseInt(port.value);
            } catch (NumberFormatException x) {
                throw new ConfigurationException(
                    String.format(
                        "Expected integer for environment variable '%s' but got '%s'",
                        port.name,
                        port.value
                    )
                );
            }
        }

        if (credentials != null) {
            var path = Path.of(credentials.value);
            if (!provider.fileExists(path)) {
                throw new ConfigurationException(
                    String.format(
                        "Could not find the file specified in '%s'",
                        credentials.name
                    )
                );
            }

            connection = connection.mergeInto(fromJSON(provider.fileReadAllText(path)));
        }

        if (user != null) {
            connection.user = user.value;
        }

        if (pass != null) {
            connection.password = pass.value;
        }

        if (db != null) {
            if(branch != null) {
                throw new IllegalArgumentException(branch.name + " conflicts with " + db.name);
            }

            connection.database = db.value;
        }

        if(branch != null) {
            connection.branch = branch.value;
        }

        return connection;
    }

    private @NotNull EdgeDBConnection mergeInto(@NotNull EdgeDBConnection other) {
        if (other.tlsSecurity == null) {
            other.tlsSecurity = this.tlsSecurity;
        }

        if (other.database == null) {
            other.database = this.database;
        }

        if (other.hostname == null) {
            other.hostname = this.hostname;
        }

        if (other.password == null) {
            other.password = this.password;
        }

        if (other.tlsCertificateAuthority == null) {
            other.tlsCertificateAuthority = this.tlsCertificateAuthority;
        }

        if (other.port == null) {
            other.port = this.port;
        }

        if (other.user == null) {
            other.user = this.user;
        }

        return other;
    }

    private static @NotNull EdgeDBConnection fromJSON(String json) throws JsonProcessingException {
        return mapper.readValue(json, EdgeDBConnection.class);
    }

    private static void setArgument(
        @NotNull EdgeDBConnection connection,
        @NotNull String name,
        @NotNull String value,
        @NotNull SystemProvider provider
    ) throws ConfigurationException, IllegalArgumentException, IOException {

        if (StringsUtil.isNullOrEmpty(value))
            return;

        switch (name) {
            case "database":
            case "branch":
                if (connection.database != null)
                    throw new IllegalArgumentException("Database ambiguity mismatch");

                connection.database = value;
                break;
            case "port":
                if (connection.port != null) {
                    throw new IllegalArgumentException("Port ambiguity mismatch");
                }

                try {
                    connection.port = Integer.parseInt(value);
                } catch (NumberFormatException e) {
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
                var path = Path.of(value);

                if (!provider.fileExists(path)) {
                    throw new FileNotFoundException("The specified tls_cert_file file was not found");
                }

                connection.tlsCertificateAuthority = provider.fileReadAllText(path);
                break;
            case "tls_security":
                var security = EnumsUtil.searchEnum(TLSSecurityMode.class, value);

                if (security == null) {
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
     *
     * @return A DSN string representing the parameters within the current {@linkplain EdgeDBConnection}.
     */
    @Override
    public @NotNull String toString() {
        var sb = new StringBuilder("edgedb://");

        this.getUsername();
        sb.append(this.getUsername());

        if (this.getPassword() != null) {
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
         *
         * @param user The new username.
         * @return The current builder.
         */
        public @NotNull Builder withUser(String user) {
            this.user = user;
            return this;
        }

        /**
         * Sets the connections' password.
         *
         * @param password The new password.
         * @return The current builder.
         */
        public @NotNull Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the connections' database.
         *
         * @param database The new database for the connection.
         * @return The current builder.
         */
        public @NotNull Builder withDatabase(String database) {
            this.database = database;
            return this;
        }

        /**
         * Sets the connections' hostname
         *
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
         *
         * @param port The new port for the connection.
         * @return The current builder.
         */
        public @NotNull Builder withPort(Integer port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the connections' tls certificate authority.
         *
         * @param tlsca The new tls certificate authority.
         * @return The current builder.
         */
        public @NotNull Builder withTlsca(String tlsca) {
            this.tlsca = tlsca;
            return this;
        }

        /**
         * Sets the connections' tls security policy.
         *
         * @param tlsSecurity The new tls security policy.
         * @return The current builder.
         */
        public @NotNull Builder withTlsSecurity(TLSSecurityMode tlsSecurity) {
            this.tlsSecurity = tlsSecurity;
            return this;
        }

        /**
         * Constructs a {@linkplain EdgeDBConnection} from the current builder.
         *
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
