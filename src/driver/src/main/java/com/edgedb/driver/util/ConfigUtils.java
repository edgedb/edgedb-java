package com.edgedb.driver.util;

import com.edgedb.driver.TLSSecurityMode;
import com.edgedb.driver.EdgeDBConnection.WaitTime;
import com.edgedb.driver.abstractions.OSType;
import com.edgedb.driver.abstractions.SystemProvider;
import com.edgedb.driver.internal.DefaultSystemProvider;
import com.edgedb.driver.util.JsonUtils.AsStringDeserializer;
import com.edgedb.driver.datatypes.internal.CloudProfile;
import com.edgedb.driver.exceptions.ConfigurationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigUtils {
    private static final SystemProvider DEFAULT_SYSTEM_PROVIDER = new DefaultSystemProvider();
    private static final String XDG_CONFIG_HOME = "XDG_CONFIG_HOME";
    private static final MessageDigest SHA1;

    static {
        try {
            SHA1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static final JsonMapper mapper = JsonMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build();

    public static @NotNull SystemProvider getDefaultSystemProvider() {
        return DEFAULT_SYSTEM_PROVIDER;
    }

    public static @NotNull Path getInstanceProjectDirectory(
        @NotNull Path projectDir,
        @NotNull SystemProvider provider
    ) {
        Path fullPath = provider.getFullPath(projectDir);
        var baseName = projectDir.getName(projectDir.getNameCount() - 1);

        if (provider.isOSPlatform(OSType.WINDOWS) && !fullPath.startsWith("\\\\")) {
            fullPath = provider.combinePaths(Path.of("\\\\?\\"), fullPath);
        }

        var hash = HexUtils.byteArrayToHexString(SHA1.digest(
            fullPath.toString().getBytes(StandardCharsets.UTF_8)
        ));

        return provider.combinePaths(
            getEdgeDBConfigDir(provider),
            "projects",
            String.format("%s-%s", baseName, hash.toLowerCase())
        );
    }

    public static @NotNull Path getEdgeDBConfigDir(@NotNull SystemProvider provider) {

        return provider.isOSPlatform(OSType.WINDOWS)
                ? provider.combinePaths(getEdgeDBBasePath(provider), "config")
                : getEdgeDBBasePath(provider);
    }

    public static @NotNull Path getCredentialsDir(@NotNull SystemProvider provider) {

        return provider.combinePaths(getEdgeDBConfigDir(provider), "credentials");
    }

    private static @NotNull Path getEdgeDBBasePath(@NotNull SystemProvider provider) {

        var basePath = getEdgeDBKnownBasePath(provider);
        return provider.directoryExists(basePath)
                ? basePath
                : provider.combinePaths(provider.getHomeDir(), ".edgedb");
    }

    private static @NotNull Path getEdgeDBKnownBasePath(@NotNull SystemProvider provider) {

        if (provider.isOSPlatform(OSType.WINDOWS)) {
           return provider.combinePaths(provider.getHomeDir(), "AppData", "Local", "EdgeDB");
        }
        else if (provider.isOSPlatform(OSType.OSX)) {
            return provider.combinePaths(provider.getHomeDir(), "Library", "Application Support", "edgedb");
        }
        else {
            @Nullable String xdgConfigDirString = provider.getEnvVariable(XDG_CONFIG_HOME);
            @Nullable Path xdgConfigDir = null;

            if (xdgConfigDirString != null) {
                xdgConfigDir = Path.of(xdgConfigDirString);
            }

            if (xdgConfigDir == null || !provider.isRooted(xdgConfigDir)) {
                xdgConfigDir = provider.combinePaths(provider.getHomeDir(), ".config");
            }

            return provider.combinePaths(xdgConfigDir, "edgedb");
        }
    }

    public static @Nullable String tryResolveProjectDatabase(
        @NotNull Path stashDir,
        @NotNull SystemProvider provider
    ) throws IOException {
        if(!provider.fileExists(stashDir)) {
            return null;
        }

        var databasePath = stashDir.resolve("database");

        if(!provider.fileExists(databasePath)) {
            return null;
        }

        return provider.fileReadAllText(databasePath);
    }

    public static final class CloudInstanceDetails {
        private final @Nullable String profile;
        private final @Nullable String linkedInstanceName;
        public CloudInstanceDetails(@Nullable String profile, @Nullable String linkedInstanceName) {
            this.profile = profile;
            this.linkedInstanceName = linkedInstanceName;
        }

        public @Nullable String getProfile() {
            return profile;
        }

        public @Nullable String getLinkedInstanceName() {
            return linkedInstanceName;
        }
    }

    public static @Nullable CloudInstanceDetails tryResolveInstanceCloudProfile(
        Path stashDir,
        @Nullable SystemProvider provider
    ) throws IOException {
        if(!provider.directoryExists(stashDir)) {
            return null;
        }

        String profile = null;
        String linkedInstanceName = null;

        var cloudProfilePath = stashDir.resolve("cloud-profile");

        if(provider.fileExists(cloudProfilePath)) {
            profile = provider.fileReadAllText(cloudProfilePath);
        }

        var linkedInstancePath = stashDir.resolve("instance-name");

        if(provider.fileExists(linkedInstancePath)) {
            linkedInstanceName = provider.fileReadAllText(linkedInstancePath);
        }

        return profile == null && linkedInstanceName == null
            ? null
            : new CloudInstanceDetails(profile, linkedInstanceName);
    }

    public static @NotNull CloudProfile readCloudProfile(
        String profile,
        @NotNull SystemProvider provider
    ) throws ConfigurationException, IOException {
        var profilePath = provider.combinePaths(
            getEdgeDBConfigDir(provider),
            "cloud-credentials",
            profile + ".json"
        );

        if(!provider.fileExists(profilePath)) {
            throw new ConfigurationException(
                String.format("Unknown cloud profile '%s'", profile)
            );
        }

        return mapper.readValue(provider.fileReadAllText(profilePath), CloudProfile.class);
    }

    //#region ResolvedFields

    /**
     * A json readable representation of an EdgeDBConnection.
     * When using Credentials to create an EdgeDBConnection, the data must conform to this type.
     */
    public static final class ConnectionCredentials {

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

        @JsonProperty("tls_ca")
        public @Nullable String tlsCertificateAuthority;

        @JsonProperty("tls_security")
        public @Nullable String tlsSecurity;

        public static ConnectionCredentials fromJson(String json) throws JsonProcessingException {
            return mapper.readValue(json, ConnectionCredentials.class);
        }
    }

    public static final class DatabaseOrBranch {
        private final @Nullable String database;
        private final @Nullable String branch;
        private DatabaseOrBranch(@Nullable String database, @Nullable String branch) {
            this.database = database;
            this.branch = branch;
        }

        public static DatabaseOrBranch ofDatabase(@NotNull String value) {
            return new DatabaseOrBranch(value, null);
        }
        public static DatabaseOrBranch ofBranch(@NotNull String value) {
            return new DatabaseOrBranch(null, value);
        }

        public @Nullable String getDatabase() {
            return database;
        }

        public @Nullable String getBranch() {
            return branch;
        }
    }

    public static final class ResolvedField<T> {
        private final @Nullable T value;
        private final @Nullable ConfigurationException error;

        private ResolvedField(@Nullable T value, @Nullable ConfigurationException error) {
            this.value = value;
            this.error = error;
        }

        public static @NotNull <T>ResolvedField<T> valid(@NotNull T value) {
            return new ResolvedField<T>(value, null);
        }
        public static @NotNull <T>ResolvedField<T> invalid(@NotNull ConfigurationException error) {
            return new ResolvedField<T>(null, error);
        }

        public final @Nullable T getValue() {
            return value;
        }

        public @Nullable ConfigurationException getError() {
            return error;
        }

        public <U> @NotNull ResolvedField<U> convert(Function<T, ResolvedField<U>> func) {
            if (error != null) {
                return ResolvedField.invalid(error);
            }
            else {
                return func.apply(value);
            }
        }
    }

    public static @Nullable <T> T tryGetFieldValue(@Nullable ResolvedField<T> field) {
        if (field == null) {
            return null;
        }

        return field.getValue();
    }

    public static @Nullable <T> T checkAndGetFieldValue(
        @Nullable ResolvedField<T> field
    ) throws ConfigurationException {
        return checkAndGetFieldValue(field, null);
    }

    @FunctionalInterface
    public interface Checker<T> {
        void accept(T t) throws ConfigurationException;
    }

    public static @Nullable <T> T checkAndGetFieldValue(
        @Nullable ResolvedField<T> field,
        @Nullable Checker<T> checker
    ) throws ConfigurationException {
        if (field == null) {
            return null;
        }

        if (field.getValue() != null && checker != null) {
            checker.accept(field.getValue());
        }

        if (field.getError() != null) {
            throw field.getError();
        }

        return field.getValue();
    }

    public static <T> @Nullable ResolvedField<T> mergeField(
        @Nullable ResolvedField<T> to,
        @Nullable ResolvedField<T> from
    ) {
        if (to == null)
        {
            return from;
        }
        else if (from != null && from.getValue() != null)
        {
            return from;
        }
        else
        {
            return to;
        }
    }

    public static HashMap<String, ResolvedField<String>> addServerSettingField(
        HashMap<String, ResolvedField<String>> serverSettings,
        String key,
        ResolvedField<String> field
    ) {
        if (serverSettings.containsKey(key))
        {
            serverSettings.put(key, mergeField(serverSettings.get(key), field));
        }
        else
        {
            serverSettings.put(key, field);
        }

        return serverSettings;
    }

    public static HashMap<String, String> checkAndGetServerSettings(
        HashMap<String, ResolvedField<String>> serverSettings
    ) throws ConfigurationException {
        HashMap<String, String> result = new HashMap<String, String>();
        for (Map.Entry<String, ResolvedField<String>> entry : serverSettings.entrySet())
        {
            result.put(entry.getKey(), checkAndGetFieldValue(entry.getValue()));
        }

        return result;
    }

    public static final class ResolvedFields {
        public @Nullable ResolvedField<String> host = null;
        public @Nullable ResolvedField<Integer> port = null;
        public @Nullable ResolvedField<DatabaseOrBranch> databaseOrBranch = null;
        public @Nullable ResolvedField<String> user = null;
        public @Nullable ResolvedField<String> password = null;
        public @Nullable ResolvedField<String> secretKey = null;
        public @Nullable ResolvedField<String> tlsCertificateAuthority = null;
        public @Nullable ResolvedField<TLSSecurityMode> tlsSecurity = null;
        public @Nullable ResolvedField<String> tlsServerName = null;
        public @Nullable ResolvedField<WaitTime> waitUntilAvailable = null;
        public HashMap<String, ResolvedField<String>> serverSettings = new HashMap<String, ResolvedField<String>>();

        public void mergeFrom(ResolvedFields other) {
            host = mergeField(host, other.host);
            port = mergeField(port, other.port);
            databaseOrBranch = mergeField(databaseOrBranch, other.databaseOrBranch);
            user = mergeField(user, other.user);
            password = mergeField(password, other.password);
            secretKey = mergeField(secretKey, other.secretKey);
            tlsCertificateAuthority = mergeField(tlsCertificateAuthority, other.tlsCertificateAuthority);
            tlsSecurity = mergeField(tlsSecurity, other.tlsSecurity);
            tlsServerName = mergeField(tlsServerName, other.tlsServerName);
            waitUntilAvailable = mergeField(waitUntilAvailable, other.waitUntilAvailable);

            for (Map.Entry<String, ResolvedField<String>> entry : other.serverSettings.entrySet()) {
                serverSettings = addServerSettingField(serverSettings, entry.getKey(), entry.getValue());
            }
        }

        public boolean isEmpty() {
            return host == null
                && port == null
                && databaseOrBranch == null
                && user == null
                && password == null
                && secretKey == null
                && tlsCertificateAuthority == null
                && tlsSecurity == null
                && tlsServerName == null
                && waitUntilAvailable == null
                && serverSettings.size() == 0;
        }

        public static ResolvedFields fromCredentials(ConnectionCredentials credentials) {
            ResolvedFields result = new ResolvedFields();
    
            if (credentials.host != null) {
                result.host = ResolvedField.valid(credentials.host);
            }
            if (credentials.port != null) {
                result.port = mergeField(result.port, parsePort(credentials.port));
            }
            if (credentials.database != null) {
                result.databaseOrBranch = ResolvedField.valid(DatabaseOrBranch.ofDatabase(credentials.database));
            }
            if (credentials.branch != null) {
                result.databaseOrBranch = ResolvedField.valid(DatabaseOrBranch.ofBranch(credentials.branch));
            }
            if (credentials.user != null) {
                result.user = ResolvedField.valid(credentials.user);
            }
            if (credentials.password != null) {
                result.password = ResolvedField.valid(credentials.password);
            }
            if (credentials.tlsCertificateAuthority != null) {
                result.tlsCertificateAuthority = ResolvedField.valid(credentials.tlsCertificateAuthority);
            }
            if (credentials.tlsSecurity != null) {
                result.tlsSecurity = parseTLSSecurityMode(credentials.tlsSecurity);
            }
    
            return result;
        }
    }

    //#endregion

    //#region Parse Functions

    public static @Nullable ResolvedField<Integer> parsePort(@NotNull String text) {
        if (text.startsWith("tcp://")) {
            return null;
        }

        try {
            return ResolvedField.valid(Integer.parseInt(text));
        }
        catch (NumberFormatException e) {
            return ResolvedField.invalid(
                new ConfigurationException(String.format(
                    "Invalid port: \"%s\", not an integer",
                    text
                ))
            );
        }
    }

    public static @NotNull ResolvedField<TLSSecurityMode> parseTLSSecurityMode(@NotNull String text) {
        TLSSecurityMode result = TLSSecurityMode.fromString(text);
        if (result != null) {
            return ResolvedField.valid(result);
        }
        else
        {
            return ResolvedField.invalid(
                new ConfigurationException(String.format(
                    "Invalid TLS Security: \"%s\", "
                    + "must be one of \"insecure\", \"no_host_verification\", \"strict\", or \"default\"",
                    text
                ))
            );
        }
    }

    private static final Pattern _isoUnitlessHours = Pattern.compile(
        "^(-?\\d+|-?\\d+\\.\\d*|-?\\d*\\.\\d+)$"
    );
    private static final Pattern _isoTimeWithUnits = Pattern.compile(
        "(?<Hours>(?<vh>-?\\d+|-?\\d+\\.\\d*|-?\\d*\\.\\d+)H)?"
        + "(?<Minutes>(?<vm>-?\\d+|-?\\d+\\.\\d*|-?\\d*\\.\\d+)M)?"
        + "(?<Seconds>(?<vs>-?\\d+|-?\\d+\\.\\d*|-?\\d*\\.\\d+)S)?"
    );
    private static final Pattern _humanHours = Pattern.compile(
        "(?<time>(?:(?<=\\s|^)-\\s*)?\\d*\\.?\\d*)\\s*(?:h(?=\\s|\\d|\\.|$)|hours?(?:\\s|$))",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern _humanMinutes = Pattern.compile(
        "(?<time>(?:(?<=\\s|^)-\\s*)?\\d*\\.?\\d*)\\s*(?:m(?=\\s|\\d|\\.|$)|minutes?(?:\\s|$))",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern _humanSeconds = Pattern.compile(
        "(?<time>(?:(?<=\\s|^)-\\s*)?\\d*\\.?\\d*)\\s*(?:s(?=\\s|\\d|\\.|$)|seconds?(?:\\s|$))",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern _humanMilliseconds = Pattern.compile(
        "(?<time>(?:(?<=\\s|^)-\\s*)?\\d*\\.?\\d*)\\s*(?:ms(?=\\s|\\d|\\.|$)|milliseconds?(?:\\s|$))",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern _humanMicroseconds = Pattern.compile(
        "(?<time>(?:(?<=\\s|^)-\\s*)?\\d*\\.?\\d*)\\s*(?:us(\\s|\\d|\\.|$)|microseconds?(?:\\s|$))",
        Pattern.CASE_INSENSITIVE
    );

    public static @NotNull ResolvedField<WaitTime> parseWaitUntilAvailable(
        @NotNull String text
    ) {

        String originalText = text;

        if (text.startsWith("PT")) {
            // ISO duration
            text = text.substring(2);
            Matcher matcher = _isoUnitlessHours.matcher(text);
            if (matcher.find()) {
                double hours = Double.parseDouble(matcher.group(0));
                return ResolvedField.valid(new WaitTime((long)(hours * 3600e6), TimeUnit.MICROSECONDS));
            }

            matcher = _isoTimeWithUnits.matcher(text);
            if (matcher.find()) {
                class IsoDurationReader {
                    public WaitTime waitTime = new WaitTime(0l, TimeUnit.MICROSECONDS);

                    public @NotNull String read(
                        @NotNull Matcher matcher,
                        @NotNull String groupName,
                        @NotNull String valueName,
                        double factor,
                        @NotNull String text
                    ) {
                        String groupValue = matcher.group(valueName);
                        if (groupValue == null || groupValue.isEmpty()) { return text; }

                        long time = (long)(Double.parseDouble(groupValue) * factor);
                        waitTime = new WaitTime(waitTime.value + time, TimeUnit.MICROSECONDS);

                        return text.replace(matcher.group(groupName), "");
                    }
                }
                IsoDurationReader reader = new IsoDurationReader();

                text = reader.read(matcher, "Hours", "vh", 3600e6, text);
                text = reader.read(matcher, "Minutes", "vm", 60e6, text);
                text = reader.read(matcher, "Seconds", "vs", 1e6, text);
                if (text.isEmpty()) {
                    return ResolvedField.valid(reader.waitTime);
                }
            }
        }
        else {
            // human duration
            class HumanDurationReader {
                public boolean found = false;
                public WaitTime waitTime = new WaitTime(0l, TimeUnit.HOURS);

                public @NotNull String read(
                    @NotNull Pattern pattern,
                    double factor,
                    @NotNull String text
                ) {
                    Matcher matcher = pattern.matcher(text);
                    if (!matcher.find()) {
                        return text;
                    }

                    String timeText = matcher.group("time");
                    if (timeText == null || timeText.isEmpty()) {
                        return text;
                    }
    
                    timeText = timeText.replaceAll("\\s+", "");
                    if (timeText.isEmpty() || timeText.endsWith(".") || timeText.startsWith("-.")) {
                        return text;
                    }
    
                    found = true;

                    long time = (long)(Double.parseDouble(timeText) * factor);
                    waitTime = new WaitTime(waitTime.value + time, TimeUnit.MICROSECONDS);

                    return text.replaceFirst(matcher.group(), "");
                }
            }
            HumanDurationReader reader = new HumanDurationReader();

            text = reader.read(_humanHours, 3600e6, text);
            text = reader.read(_humanMinutes, 60e6, text);
            text = reader.read(_humanSeconds, 1e6, text);
            text = reader.read(_humanMilliseconds, 1e3, text);
            text = reader.read(_humanMicroseconds, 1, text);
            if (reader.found && text.trim().isEmpty()) {
                return ResolvedField.valid(reader.waitTime);
            }
        }

        return ResolvedField.invalid(new ConfigurationException(String.format("invalid duration %s", originalText)));
    }

    //#endregion
}
