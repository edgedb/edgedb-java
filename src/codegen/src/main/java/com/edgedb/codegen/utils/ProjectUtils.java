package com.edgedb.codegen.utils;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

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

    public static List<String> getTargetEdgeQLFiles(Path root) {
        var result = new ArrayList<String>();

        listAllFiles(root.toFile(), file ->  file.isFile() &&
                file.getPath().endsWith(".edgeql") &&
                !file.getPath().contains(Path.of("dbschema", "migrations").toString()), result);

        return result;
    }

    private static void listAllFiles(File root, Function<File, Boolean> filter, List<String> out) {
        if(!root.isDirectory()) {
            return;
        }

        for(var file : Objects.requireNonNull(root.listFiles())) {
            if(file.isDirectory()) {
                listAllFiles(file, filter, out);
                continue;
            }

            if(filter.apply(file)) {
                out.add(file.getPath());
            }
        }
    }
}
