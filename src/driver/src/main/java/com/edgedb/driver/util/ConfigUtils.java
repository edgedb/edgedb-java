package com.edgedb.driver.util;

import com.edgedb.driver.EdgeDBConnection.WaitTime;
import com.edgedb.driver.abstractions.OSType;
import com.edgedb.driver.abstractions.SystemProvider;
import com.edgedb.driver.internal.DefaultSystemProvider;
import com.edgedb.driver.datatypes.internal.CloudProfile;
import com.edgedb.driver.exceptions.ConfigurationException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigUtils {
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

    public static class CloudInstanceDetails {
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
        JsonMapper mapper,
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

    //#region Parse Functions

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

    public static @NotNull WaitTime ParseWaitUntilAvailable(
        @NotNull String text
    ) throws ConfigurationException {

        String originalText = text;

        if (text.startsWith("PT")) {
            // ISO duration
            text = text.substring(2);
            Matcher matcher = _isoUnitlessHours.matcher(text);
            if (matcher.find()) {
                double hours = Double.parseDouble(matcher.group(0));
                return new WaitTime((long)(hours * 3600e6), TimeUnit.MICROSECONDS);
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
                    return reader.waitTime;
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
                return reader.waitTime;
            }
        }

        throw new ConfigurationException(String.format("invalid duration %s", originalText));
    }

    //#endregion
}
