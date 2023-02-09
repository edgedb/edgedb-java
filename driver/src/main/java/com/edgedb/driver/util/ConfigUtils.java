package com.edgedb.driver.util;

import com.edgedb.driver.abstractions.OSType;
import com.edgedb.driver.abstractions.SystemProvider;
import com.edgedb.driver.abstractions.impl.DefaultSystemProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public static String getInstanceProjectDirectory(String projectDir) {
        return getInstanceProjectDirectory(projectDir, DEFAULT_SYSTEM_PROVIDER);
    }
    public static String getInstanceProjectDirectory(String projectDir, SystemProvider systemProvider) {
        var provider = systemProvider == null ? DEFAULT_SYSTEM_PROVIDER : systemProvider;

        var fullPath = provider.getFullPath(projectDir);
        var baseName = CollectionUtils.last(projectDir.split(Pattern.quote(provider.getDirectorySeparator())));

        if (provider.isOSPlatform(OSType.WINDOWS) && !fullPath.startsWith("\\\\")) {
            fullPath = "\\\\?\\" + fullPath;
        }

        var hash = HexUtils.byteArrayToHexString(SHA1.digest(fullPath.getBytes(StandardCharsets.UTF_8)));

        return provider.combinePaths(getEdgeDBConfigDir(provider), "projects", String.format("%s-%s", baseName, hash.toLowerCase()));
    }

    public static String getEdgeDBConfigDir(SystemProvider systemProvider) {
        var provider = systemProvider == null ? DEFAULT_SYSTEM_PROVIDER : systemProvider;

        return provider.isOSPlatform(OSType.WINDOWS)
                ? provider.combinePaths(getEdgeDBBasePath(provider), "config")
                : getEdgeDBBasePath(provider);
    }

    public static String getCredentialsDir() {
        return getCredentialsDir(DEFAULT_SYSTEM_PROVIDER);
    }
    public static String getCredentialsDir(SystemProvider systemProvider) {
        var provider = systemProvider == null ? DEFAULT_SYSTEM_PROVIDER : systemProvider;

        return provider.combinePaths(getEdgeDBConfigDir(provider), "credentials");
    }

    private static String getEdgeDBBasePath(SystemProvider systemProvider) {
        var provider = systemProvider == null ? DEFAULT_SYSTEM_PROVIDER : systemProvider;

        var basePath = getEdgeDBKnownBasePath(provider);
        return provider.directoryExists(basePath)
                ? basePath
                : provider.combinePaths(provider.getHomeDir(), ".edgedb");
    }
    private static String getEdgeDBKnownBasePath(SystemProvider systemProvider) {
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
}
