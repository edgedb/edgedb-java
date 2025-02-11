package com.edgedb.driver.internal;

import com.edgedb.driver.abstractions.OSType;
import com.edgedb.driver.abstractions.SystemProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class BaseDefaultSystemProvider implements SystemProvider {
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
    public boolean isOSPlatform(@NotNull OSType platform) {
        return osType == platform;
    }

    @Override
    public @NotNull String getDirectorySeparator() {
        return System.getProperty("file.separator");
    }

    @Override
    public @NotNull Path getHomeDir() {
        return Paths.get(System.getProperty("user.home"));
    }

    @Override
    public @NotNull Path getCurrentDirectory() {
        return Paths.get(System.getProperty("user.dir"));
    }

    @Override
    public @NotNull Path combinePaths(@NotNull Path head, @NotNull Path... tail) {
        
        for (Path tail_element : tail) {
            head = head.resolve(tail_element);
        }

        return head;
    }

    @Override
    public @NotNull Path combinePaths(@NotNull Path head, @NotNull String... tail) {
        
        for (String tail_element : tail) {
            head = head.resolve(tail_element);
        }

        return head;
    }

    @Override
    public @NotNull Path getFullPath(@NotNull Path path) {
        return path.toAbsolutePath();
    }

    @Override
    public boolean directoryExists(@NotNull Path dir) {
        return Files.exists(dir) && Files.isDirectory(dir);
    }

    @Override
    public boolean isRooted(@NotNull Path path) {
        return path.isAbsolute();
    }

    @Override
    public @Nullable String getEnvVariable(@NotNull String name) {
        return System.getenv(name);
    }

    @Override
    public boolean fileExists(@NotNull Path path) {
        return Files.exists(path) && Files.isRegularFile(path);
    }

    @Override
    public @NotNull String fileReadAllText(@NotNull Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Override
    public void writeWarning(@NotNull String message) {
    }
}
