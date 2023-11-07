package com.edgedb.codegen.generator.types;

import java.nio.file.Path;
import java.util.Collection;

public final class GeneratedFileInfo {
    private final Path generatedPath;
    private final Collection<Path> edgeqlReferences;

    public GeneratedFileInfo(Path generatedPath, Collection<Path> edgeqlReferences) {
        this.generatedPath = generatedPath;
        this.edgeqlReferences = edgeqlReferences;
    }

    public Path getGeneratedPath() {
        return generatedPath;
    }

    public Collection<Path> getEdgeQLReferences() {
        return edgeqlReferences;
    }
}
