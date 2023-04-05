package com.edgedb.abstractions;

public interface SystemProvider {
    String getHomeDir();

    boolean isOSPlatform(OSType platform);

    String combinePaths(String path, String... paths);

    String getFullPath(String path);

    String getDirectorySeparator();

    boolean directoryExists(String path);

    boolean isRooted(String path);

    String getEnvVariable(String name);
}