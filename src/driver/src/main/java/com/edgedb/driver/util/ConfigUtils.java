package com.edgedb.driver.util;

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
}
