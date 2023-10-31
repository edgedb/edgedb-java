package com.edgedb.codegen.generator;

import java.nio.file.Path;

public class GeneratorContext {
    public final Path outputDirectory;
    public final String packageName;
    public final CodeGenerator generator;

    public GeneratorContext(Path outputDirectory, String packageName, CodeGenerator generator) {
        this.outputDirectory = outputDirectory;
        this.packageName = packageName;
        this.generator = generator;
    }
}
