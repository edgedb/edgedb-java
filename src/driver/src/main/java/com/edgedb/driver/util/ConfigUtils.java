package com.edgedb.driver.util;

import com.edgedb.driver.abstractions.OSType;
import com.edgedb.driver.abstractions.SystemProvider;
import com.edgedb.driver.abstractions.impl.DefaultSystemProvider;
import com.edgedb.driver.datatypes.internal.CloudProfile;
import com.edgedb.driver.exceptions.ConfigurationException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
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

    public static String getInstanceProjectDirectory(@NotNull String projectDir) {
        return getInstanceProjectDirectory(projectDir, DEFAULT_SYSTEM_PROVIDER);
    }
    public static String getInstanceProjectDirectory(@NotNull String projectDir, @Nullable SystemProvider systemProvider) {
        var provider = systemProvider == null ? DEFAULT_SYSTEM_PROVIDER : systemProvider;

        var fullPath = provider.getFullPath(projectDir);
        var baseName = CollectionUtils.last(projectDir.split(Pattern.quote(provider.getDirectorySeparator())));

        if (provider.isOSPlatform(OSType.WINDOWS) && !fullPath.startsWith("\\\\")) {
            fullPath = "\\\\?\\" + fullPath;
        }

        var hash = HexUtils.byteArrayToHexString(SHA1.digest(fullPath.getBytes(StandardCharsets.UTF_8)));

        return provider.combinePaths(getEdgeDBConfigDir(provider), "projects", String.format("%s-%s", baseName, hash.toLowerCase()));
    }

    public static String getEdgeDBConfigDir(@Nullable SystemProvider systemProvider) {
        var provider = systemProvider == null ? DEFAULT_SYSTEM_PROVIDER : systemProvider;

        return provider.isOSPlatform(OSType.WINDOWS)
                ? provider.combinePaths(getEdgeDBBasePath(provider), "config")
                : getEdgeDBBasePath(provider);
    }

    public static String getCredentialsDir() {
        return getCredentialsDir(DEFAULT_SYSTEM_PROVIDER);
    }
    public static String getCredentialsDir(@Nullable SystemProvider systemProvider) {
        var provider = systemProvider == null ? DEFAULT_SYSTEM_PROVIDER : systemProvider;

        return provider.combinePaths(getEdgeDBConfigDir(provider), "credentials");
    }

    private static String getEdgeDBBasePath(@Nullable SystemProvider systemProvider) {
        var provider = systemProvider == null ? DEFAULT_SYSTEM_PROVIDER : systemProvider;

        var basePath = getEdgeDBKnownBasePath(provider);
        return provider.directoryExists(basePath)
                ? basePath
                : provider.combinePaths(provider.getHomeDir(), ".edgedb");
    }

    private static String getEdgeDBKnownBasePath(@Nullable SystemProvider systemProvider) {
        var provider = systemProvider == null ? DEFAULT_SYSTEM_PROVIDER : systemProvider;

        if (provider.isOSPlatform(OSType.WINDOWS)) {
           return provider.combinePaths(provider.getHomeDir(), "AppData", "Local", "EdgeDB");
        }
        else if (provider.isOSPlatform(OSType.OSX)) {
            return provider.combinePaths(provider.getHomeDir(), "Library", "Application Support", "edgedb");
        }
        else {
            var xdgConfigDir = provider.getEnvVariable(XDG_CONFIG_HOME);

            if (xdgConfigDir == null || !provider.isRooted(xdgConfigDir)) {
                xdgConfigDir = provider.combinePaths(provider.getHomeDir(), ".config");
            }

            return provider.combinePaths(xdgConfigDir, "edgedb");
        }
    }

    public static Optional<Path> tryResolveInstanceTOML() {
        return tryResolveInstanceTOML(Path.of(System.getProperty("user.dir")));
    }

    public static Optional<Path> tryResolveInstanceTOML(Path startDir) {
        Path dir = startDir;

        while(true) {
            var target = dir.resolve("edgedb.toml");

            if(target.toFile().exists()) {
                return Optional.of(target);
            }

            var parent = target.getParent();

            if(parent == null || !Files.exists(parent)) {
                break;
            }

            dir = parent;
        }

        return Optional.empty();
    }

    public static Optional<String> tryResolveProjectDatabase(Path stashDir) throws IOException {
        if(!Files.exists(stashDir)) {
            return Optional.empty();
        }

        var databasePath = stashDir.resolve("database");

        if(!Files.exists(databasePath)) {
            return Optional.empty();
        }

        return Optional.of(Files.readString(databasePath, StandardCharsets.UTF_8));
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

    public static Optional<CloudInstanceDetails> tryResolveInstanceCloudProfile() throws IOException {
        var instanceToml = tryResolveInstanceTOML();

        if(instanceToml.isEmpty()) {
            return Optional.empty();
        }

        var stashDir =
                getInstanceProjectDirectory(
                        getInstanceProjectDirectory(instanceToml.get().getParent().toString())
                );

        return tryResolveInstanceCloudProfile(Path.of(stashDir));
    }

    public static Optional<CloudInstanceDetails> tryResolveInstanceCloudProfile(Path stashDir) throws IOException {
        if(!Files.exists(stashDir)) {
            return Optional.empty();
        }

        String profile = null;
        String linkedInstanceName = null;

        var cloudProfilePath = stashDir.resolve("cloud-profile");

        if(Files.exists(cloudProfilePath)) {
            profile = Files.readString(cloudProfilePath);
        }

        var linkedInstancePath = stashDir.resolve("instance-name");

        if(Files.exists(linkedInstancePath)) {
            linkedInstanceName = Files.readString(linkedInstancePath);
        }

        return profile == null && linkedInstanceName == null
                ? Optional.empty()
                : Optional.of(new CloudInstanceDetails(profile, linkedInstanceName));
    }

    public static CloudProfile readCloudProfile(String profile, JsonMapper mapper)
    throws ConfigurationException, IOException {
        return readCloudProfile(profile, DEFAULT_SYSTEM_PROVIDER, mapper);
    }

    public static CloudProfile readCloudProfile(String profile, SystemProvider provider, JsonMapper mapper)
    throws ConfigurationException, IOException {
        var profilePath = Path.of(provider.combinePaths(getEdgeDBConfigDir(provider), "cloud-credentials", profile + ".json"));

        if(!Files.exists(profilePath)) {
            throw new ConfigurationException(
                    String.format("Unknown cloud profile '%s'", profile)
            );
        }

        return mapper.readValue(Files.readString(profilePath), CloudProfile.class);
    }
}
