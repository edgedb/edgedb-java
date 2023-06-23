package com.edgedb.driver.abstractions.impl;

import com.edgedb.driver.abstractions.OSType;
import com.edgedb.driver.abstractions.SystemProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

public class DefaultSystemProvider implements SystemProvider {
    private static final @NotNull OSType osType;

    static {
        String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((OS.contains("mac")) || (OS.contains("darwin"))) {
            osType = OSType.OSX;
        } else if (OS.contains("win")) {
            osType = OSType.WINDOWS;
        } else if (OS.contains("nux")) {
            osType = OSType.LINUX;
        } else {
            osType = OSType.OTHER;
        }
    }

    @Override
    public String getHomeDir() {
        return System.getProperty("user.home");
    }

    @Override
    public boolean isOSPlatform(OSType platform) {
        return osType == platform;
    }

    @Override
    public @NotNull String combinePaths(@NotNull String path, String... paths) {
        return Paths.get(path, paths).toString();
    }

    @Override
    public @NotNull String getFullPath(@NotNull String path) {
        return new File(path).getAbsolutePath();
    }

    @Override
    public String getDirectorySeparator() {
        return System.getProperty("file.separator");
    }

    @Override
    public boolean directoryExists(@NotNull String path) {
        return Files.isDirectory(Paths.get(path));
    }

    @Override
    public boolean isRooted(@NotNull String path) {
        return Paths.get(path).isAbsolute();
    }

    @Override
    public String getEnvVariable(String name) {
        return System.getenv(name);
    }
}
