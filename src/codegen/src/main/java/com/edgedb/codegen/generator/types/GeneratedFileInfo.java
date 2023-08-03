package com.edgedb.codegen.generator.types;

import java.nio.file.Path;
import java.util.Collection;

public final class GeneratedFileInfo {
    private final Path generatedPath;
    private final Collection<String> edgeqlReferences;

    public GeneratedFileInfo(Path generatedPath, Collection<String> edgeqlReferences) {
        this.generatedPath = generatedPath;
        this.edgeqlReferences = edgeqlReferences;
    }

    public Path getGeneratedPath() {
        return generatedPath;
    }

    public Collection<String> getEdgeQLReferences() {
        return edgeqlReferences;
    }
}
