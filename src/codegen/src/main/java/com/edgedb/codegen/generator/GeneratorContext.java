package com.edgedb.codegen.generator;

import java.nio.file.Path;

public class GeneratorContext {
    public final Path outputDirectory;
    public final String packageName;

    public GeneratorContext(String outputDirectory, String packageName) {
        this.outputDirectory = Path.of(outputDirectory);
        this.packageName = packageName;
    }
}
