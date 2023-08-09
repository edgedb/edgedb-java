package com.edgedb.codegen.generator;

import java.nio.file.Path;

public class GeneratorContext {
    public final Path outputDirectory;
    public final String packageName;

    public GeneratorContext(Path outputDirectory, String packageName) {
        this.outputDirectory = outputDirectory;
        this.packageName = packageName;
    }
}
