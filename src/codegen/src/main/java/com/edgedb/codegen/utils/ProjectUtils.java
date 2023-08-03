package com.edgedb.codegen.utils;

import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectUtils {
    public static Path getProjectRoot() throws FileNotFoundException {
        return getProjectRoot(null);
    }

    public static Path getProjectRoot(@Nullable String startingDir) throws FileNotFoundException {
        var dir = startingDir == null
                ? Paths.get(System.getProperty("user.dir"))
                : Path.of(startingDir);

        while(true) {
            var target = dir.resolve("edgedb.toml");

            if (Files.exists(target)) {
                return dir;
            }

            var parent = dir.getParent();

            if(parent == null || !Files.exists(parent)) {
                throw new FileNotFoundException("Couldn't resolve edgedb.toml file");
            }

            dir = parent;
        }
    }

    public static String[] getTargetEdgeQLFiles(Path root) {
        return root.toFile().list((pathname, name) -> pathname.isFile() &&
                pathname.getPath().endsWith(".edgeql") &&
                !pathname.getPath().contains(Path.of("dbschema", "migrations").toString()));
    }
}
