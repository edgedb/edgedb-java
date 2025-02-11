package com.edgedb.driver.abstractions;

import java.io.IOException;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SystemProvider {
    public boolean isOSPlatform(@NotNull OSType platform);
    public @NotNull String getDirectorySeparator();
    public @NotNull Path getHomeDir();
    public @NotNull Path getCurrentDirectory();
    public @NotNull Path combinePaths(@NotNull Path head, @NotNull Path... tail);
    public @NotNull Path combinePaths(@NotNull Path head, @NotNull String... tail);
    public @NotNull Path getFullPath(@NotNull Path path);
    public boolean directoryExists(@NotNull Path dir);
    public boolean isRooted(@NotNull Path path);
    public @Nullable String getEnvVariable(@NotNull String name);
    public boolean fileExists(@NotNull Path path);
    public @NotNull String fileReadAllText(@NotNull Path path) throws IOException;
    public void writeWarning(@NotNull String message);
}
