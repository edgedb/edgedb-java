package com.edgedb.driver;

import com.edgedb.driver.abstractions.SystemProvider;
import com.edgedb.driver.abstractions.SystemProvider.GelEnvVar;
import com.edgedb.driver.datatypes.internal.CloudProfile;
import com.edgedb.driver.exceptions.ConfigurationException;
import com.edgedb.driver.util.*;
import com.edgedb.driver.util.ConfigUtils.ConnectionCredentials;
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
    public @NotNull String getPassword() {
        return password == null ? _defaultPassword : password;
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
    private static final String _defaultPassword = "";

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

    /**
     * Gets additional settings for the server connection
     * 
     * This currently has no effect.
     *
     * @return The TLS server name to be used if present; otherwise {@code null}.
     */
    public @Nullable HashMap<String, String> getServerSettings() {
        return serverSettings;
    }

    private @NotNull HashMap<String, String> serverSettings = new HashMap<String, String>();

    //#endregion

    //#region Builder

    /**
     * A builder class used to construct a {@linkplain EdgeDBConnection}
     */
    public static final class Builder2 {
        // Primary args
        // These can set host/port of the connection.
        private @Nullable String instance;
        private @Nullable String dsn;
        private @Nullable String credentials;
        private @Nullable Path credentialsFile;
        private @Nullable String host;
        private @Nullable Integer port;

        // Secondary args
        private @Nullable String database;
        private @Nullable String branch;
        private @Nullable String user;
        private @Nullable String password;
        private @Nullable String secretKey;
        private @Nullable String tlsCertificateAuthority;
        private @Nullable Path tlsCertificateAuthorityFile;
        private @Nullable TLSSecurityMode tlsSecurity;
        private @Nullable String tlsServerName;
        private @Nullable String waitUntilAvailable;
        private @Nullable HashMap<String, String> serverSettings;

        /**
         * Sets the connections' instance.
         *
         * @param instance The new instance name.
         * @return The current builder.
         */
        public @NotNull Builder2 withInstance(@NotNull String instance) {
            this.instance = instance;
            return this;
        }

        /**
         * Sets the connections' dsn.
         *
         * @param dsn The new dsn.
         * @return The current builder.
         */
        public @NotNull Builder2 withDsn(@NotNull String dsn) {
            this.dsn = dsn;
            return this;
        }

        /**
         * Sets the connections' credentials.
         *
         * This should be a json string which corresponds to
         * ConnectionCredentials.
         *
         * @see ConfigUtils.ConnectionCredentials
         *
         * @param credentials The new credentials.
         * @return The current builder.
         */
        public @NotNull Builder2 withCredentials(@NotNull String credentials) {
            this.credentials = credentials;
            return this;
        }

        /**
         * Sets the connections' credentials file.
         *
         * This should be the path to a json file which corresponds to
         * ConnectionCredentials.
         *
         * @see ConfigUtils.ConnectionCredentials
         *
         * @param credentialsFile The new credentials file.
         * @return The current builder.
         */
        public @NotNull Builder2 withCredentialsFile(@NotNull Path credentialsFile) {
            this.credentialsFile = credentialsFile;
            return this;
        }

        /**
         * Sets the connections' host
         *
         * @param host The new host.
         * @return The current builder
         */
        public @NotNull Builder2 withHost(@NotNull String host) {
            this.host = host;
            return this;
        }

        /**
         * Sets the connections' port.
         *
         * @param port The new port.
         * @return The current builder.
         */
        public @NotNull Builder2 withPort(@NotNull Integer port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the connections' database.
         *
         * @param database The new database.
         * @return The current builder.
         */
        public @NotNull Builder2 withDatabase(@NotNull String database) {
            this.database = database;
            return this;
        }

        /**
         * Sets the connections' branch.
         *
         * @param branch The new branch.
         * @return The current builder.
         */
        public @NotNull Builder2 withBranch(@NotNull String branch) {
            this.branch = branch;
            return this;
        }

        /**
         * Sets the connections' username.
         *
         * @param user The new username.
         * @return The current builder.
         */
        public @NotNull Builder2 withUser(@NotNull String user) {
            this.user = user;
            return this;
        }

        /**
         * Sets the connections' password.
         *
         * @param password The new password.
         * @return The current builder.
         */
        public @NotNull Builder2 withPassword(@NotNull String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the connections' secret key.
         *
         * @param secretKey The new secret key.
         * @return The current builder.
         */
        public @NotNull Builder2 withSecretKey(@NotNull String secretKey) {
            this.secretKey = secretKey;
            return this;
        }

        /**
         * Sets the connections' tls certificate authority.
         *
         * @param tlsCertificateAuthority The new tls certificate authority.
         * @return The current builder.
         */
        public @NotNull Builder2 withTLSCertificateAuthority(@NotNull String tlsCertificateAuthority) {
            this.tlsCertificateAuthority = tlsCertificateAuthority;
            return this;
        }

        /**
         * Sets the connections' tls certificate authority.
         *
         * The certificate authority will be read from this file.
         *
         * @param tlsCertificateAuthority The new tls certificate authority file.
         * @return The current builder.
         */
        public @NotNull Builder2 withTLSCertificateAuthorityFile(@NotNull Path tlsCertificateAuthorityFile) {
            this.tlsCertificateAuthorityFile = tlsCertificateAuthorityFile;
            return this;
        }

        /**
         * Sets the connections' tls security policy.
         *
         * @param tlsSecurity The new tls security policy.
         * @return The current builder.
         */
        public @NotNull Builder2 withTLSSecurity(@NotNull TLSSecurityMode tlsSecurity) {
            this.tlsSecurity = tlsSecurity;
            return this;
        }

        /**
         * Sets the connections' tls server name.
         *
         * Overrides the value provided by Hostname
         *
         * @param tlsServerName The new tlsServerName.
         * @return The current builder.
         */
        public @NotNull Builder2 withTLSServerName(@NotNull String tlsServerName) {
            this.tlsServerName = tlsServerName;
            return this;
        }

        /**
         * Sets the number of miliseconds a client will wait for a connection to be
         * established with the server.
         *
         * @param waitUntilAvailable The new wait time.
         * @return The current builder.
         */
        public @NotNull Builder2 withWaitUntilAvailable(@NotNull String waitUntilAvailable) {
            this.waitUntilAvailable = waitUntilAvailable;
            return this;
        }

        /**
         * Sets the additional connections' server settings.
         *
         * This currently has no effect.
         *
         * @param serverSettings The new serverSettings.
         * @return The current builder.
         */
        public @NotNull Builder2 withServerSettings(@NotNull HashMap<String, String> serverSettings) {
            this.serverSettings = serverSettings;
            return this;
        }

        /*
         * Checks whether this builder has any fields set.
         * 
         * @return Whether any fields are set.
         */
        public boolean isEmpty() {
            return
                instance == null
                && dsn == null
                && host == null
                && port == null
                && database == null
                && branch == null
                && user == null
                && password == null
                && secretKey == null
                && credentials == null
                && credentialsFile == null
                && tlsCertificateAuthority == null
                && tlsCertificateAuthorityFile == null
                && tlsSecurity == null
                && tlsServerName == null
                && waitUntilAvailable == null
                && serverSettings == null;
        }

        /**
         * Constructs a {@linkplain EdgeDBConnection} from the current builder.
         *
         * @return A {@linkplain EdgeDBConnection} that represents the current builder.
         */
        public @NotNull EdgeDBConnection build(
        ) throws ConfigurationException, IOException {
            return fromBuilder(this, ConfigUtils.getDefaultSystemProvider());
        }
    }

    private static @NotNull EdgeDBConnection fromBuilder(
        @NotNull Builder2 builder,
        @NotNull SystemProvider provider
        ) throws ConfigurationException, IOException {
        return _fromResolvedFields(
            _fromBuilder(builder, provider),
            provider
        );
    }

    private static @NotNull ResolvedFields _fromBuilder(
        @NotNull Builder2 builder,
        @NotNull SystemProvider provider
    ) throws ConfigurationException, IOException {
        ResolvedFields resolvedFields = new ResolvedFields();

        //#region Primary Options

        // First, check primary options
        // If any primary options are present, environment variables are ignored

        boolean hasPrimaryOptions = false;
        {
            // These options can set host/port and should be resolved first
            // More than one primary options should raise an error

            ConfigurationException primaryError = new ConfigurationException(
                "Connection options cannot have more than one of the following "
                + "values: \"Instance\", \"Dsn\", \"Credentials\", "
                + "\"CredentialsFile\" or \"Host\"/\"Port\"");
            // The primaryError has priority, so hold on to any other exception
            // until all primary options are processed.
            @Nullable ConfigurationException deferredPrimaryError = null;

            if (builder.instance != null) {
                if (hasPrimaryOptions) {
                    throw primaryError;
                }
                if (!builder.instance.equals("")) {
                    try {
                        var fromDSN = _fromInstanceName(builder.instance, null, provider);
                        resolvedFields.mergeFrom(fromDSN);
                    }
                    catch (ConfigurationException e) {
                        deferredPrimaryError = e;
                    }
                }
                else {
                    deferredPrimaryError = new ConfigurationException(String.format(
                        "Invalid instance name: \"%s\"",
                        builder.instance
                    ));
                }
                hasPrimaryOptions = true;
            }

            if (builder.dsn != null) {
                if (hasPrimaryOptions) {
                    throw primaryError;
                }
                var fromDSN = _fromDSN(builder.dsn, provider);
                resolvedFields.mergeFrom(fromDSN);
                hasPrimaryOptions = true;
            }

            {
                @Nullable String credentialsText = null;
                if (builder.credentials != null) {
                    if (hasPrimaryOptions) {
                        throw primaryError;
                    }
                    credentialsText = builder.credentials;
                    hasPrimaryOptions = true;
                }
                if (builder.credentialsFile != null) {
                    if (hasPrimaryOptions) {
                        throw primaryError;
                    }
                    if (provider.fileExists(builder.credentialsFile)) {
                        try {
                            credentialsText = provider.fileReadAllText(builder.credentialsFile);
                        }
                        catch (IOException e) {
                            credentialsText = "{}";
                        }
                    }
                    else {
                        deferredPrimaryError = new ConfigurationException(String.format(
                            "Invalid CredentialsFile: \"%s\", could not find file",
                            builder.credentialsFile
                        ));
                    }
                    hasPrimaryOptions = true;
                }
                if (credentialsText != null) {
                    try {
                        ConnectionCredentials credentials = mapper.readValue(
                            credentialsText,
                            ConnectionCredentials.class
                        );

                        if (credentials != null) {
                            resolvedFields.mergeFrom(
                                ConfigUtils.ResolvedFields.fromCredentials(credentials)
                            );
                        }
                    }
                    catch (JsonProcessingException e) {
                        deferredPrimaryError = new ConfigurationException(
                            "Invalid Credentials: could not parse json"
                        );
                    }
                }
            }

            {
                boolean hasHostOrPort = false;
                if (builder.host != null) {
                    if (hasPrimaryOptions) {
                        throw primaryError;
                    }
                    resolvedFields.host = ResolvedField.valid(builder.host);
                    hasHostOrPort = true;
                }
                if (builder.port != null) {
                    if (hasPrimaryOptions) {
                        throw primaryError;
                    }
                    resolvedFields.port = ResolvedField.valid(builder.port);
                    hasHostOrPort = true;
                }
                if (hasHostOrPort) {
                    hasPrimaryOptions = true;
                }
            }

            if (deferredPrimaryError != null) {
                throw deferredPrimaryError;
            }
        }

        //#endregion

        //#region Primary Env

        boolean hasPrimaryEnv = false;

        if (!hasPrimaryOptions)
        {
            // These env vars can set host/port and should be resolved first
            // More than one primary env var should raise an error

            ConfigurationException primaryError = new ConfigurationException(
                "Cannot have more than one of the following connection "
                + "environment variables: \"GEL_DSN\", \"GEL_INSTANCE\", "
                + "\"GEL_CREDENTIALS_FILE\" or \"GEL_HOST\"/\"GEL_PORT\"");
            // The primaryError has priority, so hold on to any other exception
            // until all primary env vars are processed.
            @Nullable ConfigurationException deferredPrimaryError = null;

            GelEnvVar instanceEnvVar = SystemProvider.getGelEnvVariable(provider, INSTANCE_ENV_NAME);
            if (instanceEnvVar != null) {
                if (hasPrimaryEnv) {
                    throw primaryError;
                }
                try {
                    var fromInst = _fromInstanceName(instanceEnvVar.value, null, provider);
                    resolvedFields.mergeFrom(fromInst);
                }
                catch (ConfigurationException e) {
                    deferredPrimaryError = e;
                }
                hasPrimaryEnv = true;
            }

            GelEnvVar dsnEnvVar = SystemProvider.getGelEnvVariable(provider, DSN_ENV_NAME);
            if (dsnEnvVar != null) {
                if (hasPrimaryEnv) {
                    throw primaryError;
                }
                var fromDSN = _fromDSN(dsnEnvVar.value, provider);
                resolvedFields.mergeFrom(fromDSN);
                hasPrimaryEnv = true;
            }

            GelEnvVar credentialsFileEnvVar = SystemProvider.getGelEnvVariable(provider, CREDENTIALS_FILE_ENV_NAME);
            if (credentialsFileEnvVar != null) {
                if (hasPrimaryEnv) {
                    throw primaryError;
                }
                if (provider.fileExists(Path.of(credentialsFileEnvVar.value))) {
                    try {
                        ConnectionCredentials credentials = ConnectionCredentials.fromJson(
                            provider.fileReadAllText(Path.of(credentialsFileEnvVar.value))
                        );
                        resolvedFields.mergeFrom(
                            ConfigUtils.ResolvedFields.fromCredentials(credentials)
                        );
                    }
                    catch (JsonProcessingException e) {}
                }
                else
                {
                    deferredPrimaryError = new ConfigurationException(String.format(
                        "Invalid credential file from %s: \"%s\", could not find file",
                        credentialsFileEnvVar.name,
                        credentialsFileEnvVar.value
                    ));
                }

                hasPrimaryEnv = true;
            }

            {
                boolean hasHostOrPort = false;

                GelEnvVar hostEnvVar = SystemProvider.getGelEnvVariable(provider, HOST_ENV_NAME);
                if (hostEnvVar != null) {
                    if (hasPrimaryEnv) {
                        throw primaryError;
                    }
                    resolvedFields.host = ResolvedField.valid(hostEnvVar.value);
                    hasHostOrPort = true;
                }

                GelEnvVar portEnvVar = SystemProvider.getGelEnvVariable(provider, PORT_ENV_NAME);
                if (portEnvVar != null) {
                    ResolvedField<Integer> port = ConfigUtils.parsePort(portEnvVar.value);
                    if (port != null) {
                        if (hasPrimaryEnv) { throw primaryError; }
                        resolvedFields.port = ConfigUtils.mergeField(resolvedFields.port, port);
                        hasHostOrPort = true;
                    }
                }
                if (hasHostOrPort) {
                    hasPrimaryEnv = true;
                }
            }

            if (deferredPrimaryError != null) {
                throw deferredPrimaryError;
            }
        }

        //#endregion

        //#region Toml File

        if (!hasPrimaryOptions && !hasPrimaryEnv) {
            @Nullable ResolvedFields fromToml = _resolveInstanceTOML(provider);
            if (fromToml != null) {
                resolvedFields.mergeFrom(fromToml);
            }
        }

        //#endregion

        //#region Secondary Env

        if (!hasPrimaryOptions) {
            GelEnvVar databaseEnvVar = SystemProvider.getGelEnvVariable(provider, DATABASE_ENV_NAME);
            GelEnvVar branchEnvVar = SystemProvider.getGelEnvVariable(provider, BRANCH_ENV_NAME);
            if (databaseEnvVar != null) {
                if (branchEnvVar != null) {
                    throw new ConfigurationException(String.format(
                        "Environment variables %s and %s are mutually exclusive",
                        databaseEnvVar.name,
                        branchEnvVar.name
                    ));
                }

                resolvedFields.databaseOrBranch = ResolvedField.valid(
                    DatabaseOrBranch.ofDatabase(databaseEnvVar.value)
                );
            }
            if (branchEnvVar != null) {
                resolvedFields.databaseOrBranch = ResolvedField.valid(
                    DatabaseOrBranch.ofBranch(branchEnvVar.value)
                );
            }

            GelEnvVar userEnvVar = SystemProvider.getGelEnvVariable(provider, USER_ENV_NAME);
            if (userEnvVar != null) {
                resolvedFields.user = ResolvedField.valid(userEnvVar.value);
            }

            GelEnvVar passwordEnvVar = SystemProvider.getGelEnvVariable(provider, PASSWORD_ENV_NAME);
            if (passwordEnvVar != null) {
                resolvedFields.password = ResolvedField.valid(passwordEnvVar.value);
            }

            GelEnvVar tlsCertificateAuthorityEnvVar = SystemProvider.getGelEnvVariable(provider, TLS_CA_ENV_NAME);
            if (tlsCertificateAuthorityEnvVar != null) {
                resolvedFields.tlsCertificateAuthority = ResolvedField.valid(
                    tlsCertificateAuthorityEnvVar.value
                );
            }

            {
                GelEnvVar clientSecurityEnvVar = SystemProvider.getGelEnvVariable(provider, CLIENT_SECURITY_ENV_NAME);
                GelEnvVar clientTlsSecurityEnvVar = SystemProvider.getGelEnvVariable(provider, CLIENT_TLS_SECURITY_ENV_NAME);
                TLSSecurityMode clientSecurity = null;
                TLSSecurityMode clientTlsSecurity = null;
                boolean hasDefault = false;
                if (clientSecurityEnvVar != null) {
                    clientSecurity = TLSSecurityMode.fromString(
                        clientSecurityEnvVar.value,
                        true
                    );

                    if (clientSecurity != null) {
                        if (clientSecurity == TLSSecurityMode.DEFAULT) {
                            hasDefault = true;
                        }
                        else {
                            resolvedFields.tlsSecurity = ResolvedField.valid(clientSecurity);
                        }
                    }
                    else {
                        resolvedFields.tlsSecurity = ResolvedField.invalid(
                            new ConfigurationException(String.format(
                                "Invalid TLS Security from %s: \"%s\"",
                                clientSecurityEnvVar.name,
                                clientSecurityEnvVar.value
                            ))
                        );
                    }
                }
                if (clientTlsSecurityEnvVar != null) {
                    clientTlsSecurity = TLSSecurityMode.fromString(
                        clientTlsSecurityEnvVar.value,
                        true
                    );

                    if (clientTlsSecurity != null)
                    {
                        if (clientTlsSecurity == TLSSecurityMode.DEFAULT) {
                            hasDefault = true;
                        }
                        else if (clientSecurity == TLSSecurityMode.DEFAULT) {
                            // overwrite default value
                            resolvedFields.tlsSecurity = ResolvedField.valid(clientTlsSecurity);
                        }
                        else if (clientSecurity == TLSSecurityMode.STRICT
                            && clientTlsSecurity != TLSSecurityMode.STRICT
                        ) {
                            throw new ConfigurationException(String.format(
                                "%s=strict but %s=%s. %s must be strict when %s is strict",
                                clientSecurityEnvVar.name,
                                clientTlsSecurityEnvVar.name,
                                clientTlsSecurityEnvVar.value,
                                clientTlsSecurityEnvVar.name,
                                clientSecurityEnvVar.name
                            ));
                        }
                        else {
                            // overwrite existing value
                            resolvedFields.tlsSecurity = ResolvedField.valid(clientTlsSecurity);
                        }
                    }
                    else {
                        resolvedFields.tlsSecurity = ResolvedField.invalid(
                            new ConfigurationException(String.format(
                                "Invalid TLS Security from %s: \"%s\"",
                                clientTlsSecurityEnvVar.name,
                                clientTlsSecurityEnvVar.value
                            ))
                        );
                    }
                }
                if (hasDefault && ConfigUtils.tryGetFieldValue(resolvedFields.tlsSecurity) == null) {
                    // finally, apply default value if no non-default value or error present
                    resolvedFields.tlsSecurity = ResolvedField.valid(TLSSecurityMode.DEFAULT);
                }
            }

            GelEnvVar tlsServerEnvVar = SystemProvider.getGelEnvVariable(provider, TLS_SERVER_NAME_ENV_NAME);
            if (tlsServerEnvVar != null) {
                resolvedFields.tlsServerName = ResolvedField.valid(tlsServerEnvVar.value);
            }

            GelEnvVar waitUntilAvailbleEnvVar = SystemProvider.getGelEnvVariable(provider, WAIT_UNTIL_AVAILABLE_ENV_NAME);
            if (waitUntilAvailbleEnvVar != null) {
                resolvedFields.waitUntilAvailable = ConfigUtils.parseWaitUntilAvailable(
                    waitUntilAvailbleEnvVar.value
                );
            }
        }

        //#endregion

        //#region Secondary Options

        // Finally, check secondary options
        // Secondary options should override environment variables

        if (builder.database != null && builder.branch != null) {
            throw new ConfigurationException(
                "Invalid builder: Database and Branch are mutually exclusive."
            );
        }
        else if (builder.database != null) {
            resolvedFields.databaseOrBranch = ResolvedField.valid(
                DatabaseOrBranch.ofDatabase(builder.database)
            );
        }
        else if (builder.branch != null) {
            resolvedFields.databaseOrBranch = ResolvedField.valid(
                DatabaseOrBranch.ofBranch(builder.branch)
            );
        }

        if (builder.user != null) {
            resolvedFields.user = ResolvedField.valid(builder.user);
        }
        if (builder.password != null) {
            resolvedFields.password = ResolvedField.valid(builder.password);
        }
        if (builder.secretKey != null) {
            resolvedFields.secretKey = ResolvedField.valid(builder.secretKey);
        }
        if (builder.tlsCertificateAuthority != null) {
            resolvedFields.tlsCertificateAuthority = ResolvedField.valid(builder.tlsCertificateAuthority);
        }
        if (builder.tlsCertificateAuthorityFile != null) {
            if (provider.fileExists(builder.tlsCertificateAuthorityFile)) {
                resolvedFields.tlsCertificateAuthority = ResolvedField.valid(
                    provider.fileReadAllText(builder.tlsCertificateAuthorityFile)
                );
            }
            else {
                throw new ConfigurationException(String.format(
                    "Invalid TLSCertificateAuthorityFile: \"%s\", could not find file",
                    builder.tlsCertificateAuthorityFile
                ));
            }
        }
        if (builder.tlsSecurity != null) {
            resolvedFields.tlsSecurity = ResolvedField.valid(builder.tlsSecurity);
        }
        if (builder.tlsServerName != null) {
            resolvedFields.tlsServerName = ResolvedField.valid(builder.tlsServerName);
        }
        if (builder.waitUntilAvailable != null) {
            resolvedFields.waitUntilAvailable = ConfigUtils.mergeField(
                resolvedFields.waitUntilAvailable,
                ConfigUtils.parseWaitUntilAvailable(builder.waitUntilAvailable));
        }
        if (builder.serverSettings != null) {
            for (Entry<String, String> entry : builder.serverSettings.entrySet()) {
                resolvedFields.serverSettings = ConfigUtils.addServerSettingField(
                    resolvedFields.serverSettings,
                    entry.getKey(),
                    ResolvedField.valid(entry.getValue())
                );
            }
        }

        //#endregion

        if (builder.isEmpty() && resolvedFields.isEmpty())
        {
            throw new ConfigurationException(
                "No `gel.toml` found and no connection options specified."
            );
        }

        return resolvedFields;
    }
    //#endregion

    //#region Builder Helpers

    private static EdgeDBConnection _fromResolvedFields(
        @NotNull ConfigUtils.ResolvedFields resolvedFields,
        @NotNull SystemProvider platform
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
                if (value.equals("")) {
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
                        "Invalid port: \"%d\", must be between 1 and 65535",
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
        if (branch != null && !branch.equals("")) usedParamNames.add("branch");
        if (host != null && !host.equals("")) usedParamNames.add("host");
        if (username != null && !username.equals("")) usedParamNames.add("user");
        if (password != null && !password.equals("")) usedParamNames.add("password");
        if (port != null && !port.equals("")) usedParamNames.add("port");

        // Parse query params
        HashMap<String, String> args = new HashMap<String, String>();
        if (dsnMatcher.group("params") != null) {

            if (!_dsnParamsRegex.matcher(dsnMatcher.group("params")).find()) {
                throw new ConfigurationException("Invalid DSN: could not parse query parameters");
            }

            String[] params = dsnMatcher.group("params").split("&");
            for (String param : params) {
                String[] entry = param.split("=", -1);
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

                    args.put(entry[0], entry[1]);
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
        return _fromResolvedFields(
            _fromProjectFile(path, ConfigUtils.getDefaultSystemProvider()),
            ConfigUtils.getDefaultSystemProvider()
        );
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
    private static @NotNull ResolvedFields _fromProjectFile(
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

        ResolvedFields resolvedFields = _fromInstanceName(
            instanceDetails.getLinkedInstanceName(),
            instanceDetails.getProfile(),
            provider
        );

        String database = ConfigUtils.tryResolveProjectDatabase(projectDir, provider);
        if (database != null) {
            resolvedFields.databaseOrBranch = ResolvedField.valid(DatabaseOrBranch.ofDatabase(database));
        }

        return resolvedFields;
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

        return _fromResolvedFields(
            _fromInstanceName(
                instanceName,
                null,
                ConfigUtils.getDefaultSystemProvider()
            ),
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

        return _fromResolvedFields(
            _fromInstanceName(
                instanceName,
                cloudProfile,
                ConfigUtils.getDefaultSystemProvider()
            ),
            ConfigUtils.getDefaultSystemProvider()
        );
    }

    private static @NotNull ResolvedFields _fromInstanceName(
        String instanceName,
        @Nullable String cloudProfile,
        @NotNull SystemProvider provider
    ) throws ConfigurationException, IOException {

        if (Pattern.matches("^\\w(-?\\w)*$", instanceName)) {
            var configPath = provider.combinePaths(ConfigUtils.getCredentialsDir(provider), instanceName + ".json");

            if (!provider.fileExists(configPath)) {
                throw new ConfigurationException("Config file couldn't be found at " + configPath);
            }

            try {
                ConnectionCredentials credentials = ConnectionCredentials.fromJson(
                    provider.fileReadAllText(configPath)
                );
                return ConfigUtils.ResolvedFields.fromCredentials(credentials);
            }
            catch (JsonProcessingException e) {}

        } else if (Pattern.matches("^([A-Za-z0-9](-?[A-Za-z0-9])*)/([A-Za-z0-9](-?[A-Za-z0-9])*)$", instanceName)) {
            return _parseCloudInstanceName(instanceName, null, cloudProfile, provider);
        }

        throw new ConfigurationException(
            String.format("Invalid instance name '%s'", instanceName)
        );
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
        ResolvedFields resolvedFields = _resolveInstanceTOML(ConfigUtils.getDefaultSystemProvider());
        if (resolvedFields == null) {
            throw new ConfigurationException("Couldn't resolve edgedb.toml file");
        }
        return _fromResolvedFields(resolvedFields, ConfigUtils.getDefaultSystemProvider());
    }

    private static @Nullable ResolvedFields _resolveInstanceTOML(
        @NotNull SystemProvider provider
    ) throws ConfigurationException, IOException {
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
                return null;
            }

            dir = parent;
        }
    }

    private void parseCloudInstanceName(
        @NotNull String name,
        @Nullable String cloudProfile,
        @NotNull SystemProvider provider
    ) throws ConfigurationException, IOException {

        ResolvedFields resolvedFields = _parseCloudInstanceName(name, null, cloudProfile, provider);

        setHostname(resolvedFields.host.getValue());
        setSecretKey(resolvedFields.secretKey.getValue());
    }

    private static final String _defaultCloudProfile = "default";
    private static ResolvedFields _parseCloudInstanceName(
        @NotNull String name,
        @Nullable String secretKey,
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

        if (secretKey == null) {
            if (cloudProfile == null) {
                cloudProfile = _defaultCloudProfile;
            }
            CloudProfile profile = ConfigUtils.readCloudProfile(cloudProfile, provider);

            if (profile.secretKey == null) {
                throw new ConfigurationException(String.format(
                    "Secret key in cloud profile '%s' cannot be null",
                    cloudProfile
                ));
            }

            secretKey = profile.secretKey;
        }

        var spl = secretKey.split("\\.");

        if (spl.length < 2) {
            throw new ConfigurationException("Invalid secret key: doesn't contain payload");
        }

        var json = Base64.getDecoder().decode(spl[1]);
        HashMap<String, Object> jsonData = mapper.readValue(json, new TypeReference<HashMap<String, Object>>() {});

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

        ResolvedFields result = new ResolvedFields();
        result.host = ResolvedField.valid(String.format(
            "%s--%s.c-%s.i.%s",
            spl[1],
            spl[0],
            dnsBucket,
            jsonData.get("iss")
        ));
        result.secretKey = ResolvedField.valid(secretKey);

        return result;
    }

    //#endregion

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
                _fromResolvedFields(
                    _fromInstanceName(instanceName.value, null, provider),
                    provider
                )
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
