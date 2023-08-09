package com.edgedb.codegen.generator;

import com.edgedb.codegen.utils.HashUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

public class GeneratorTargetInfo {
    public final String edgeql;
    public final String filename;
    public final Path path;
    public final String hash;

    public GeneratorTargetInfo(String edgeql, String filename, Path path, String hash) {
        this.edgeql = edgeql;
        this.filename = filename;
        this.path = path;
        this.hash = hash;
    }

    public Path getGenerationPath(GeneratorContext context) {
        return context.outputDirectory.resolve(filename + ".g.java");
    }

    public static GeneratorTargetInfo fromFile(Path path) throws IOException, NoSuchAlgorithmException {
        if(!Files.exists(path)) {
            throw new FileNotFoundException("Could not find the file " + path);
        }

        var edgeql = Files.readString(path, StandardCharsets.UTF_8);

        var hash = HashUtils.hashEdgeQL(edgeql);

        return new GeneratorTargetInfo(
                edgeql,
                path.getFileName().toString().replace(".edgeql", ""),
                path,
                hash
        );
    }
}
