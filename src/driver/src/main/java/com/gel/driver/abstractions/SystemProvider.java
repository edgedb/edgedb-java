package com.gel.driver.abstractions;

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

    public class GelEnvVar
    {
        public final @NotNull String name;
        public final @NotNull String value;
        public GelEnvVar(@NotNull String name, @NotNull String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static @Nullable GelEnvVar getGelEnvVariable(SystemProvider provider, @NotNull String key)
    {
        @NotNull String edgedbKey = String.format("EDGEDB_%s", key);
        @Nullable String edgedbVal = provider.getEnvVariable(edgedbKey);
        @NotNull String gelKey = String.format("GEL_%s", key);
        @Nullable String gelVal = provider.getEnvVariable(gelKey);
        if (edgedbVal != null && gelVal != null)
        {
            provider.writeWarning(String.format(
                "Both GEL_%s and EDGEDB_%s are set; EDGEDB_%s will be ignored",
                key,
                key,
                key
            ));
        }

        if (gelVal != null)
        {
            return new GelEnvVar(gelKey, gelVal);
        }
        else if (edgedbVal != null)
        {
            return new GelEnvVar(edgedbKey, edgedbVal);
        }
        else
        {
            return null;
        }
    }
}
