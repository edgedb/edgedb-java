package com.edgedb.testgen;

import java.io.Closeable;
import java.io.IOException;

public class CodeWriter {
    private final StringBuilder content;
    private final ScopeTracker tracker;
    private int indentLevel;

    public CodeWriter() {
        tracker = new ScopeTracker();
        content = new StringBuilder();
    }

    public void append(String line) {
        content.append(line);
    }

    public void appendLine(String line) {
        content.append(" ".repeat(indentLevel)).append(line).append(System.lineSeparator());
    }

    public void appendLine() {
        content.append(System.lineSeparator());
    }

    public Closeable beginScope(String line) {
        content.append(" ".repeat(indentLevel)).append(line);
        return beginScope();
    }

    public Closeable beginScope() {
        append(" {");
        append(System.lineSeparator());
        indentLevel += 4;
        return tracker;
    }

    public void endScope() {
        indentLevel -= 4;
        appendLine("}");
    }

    @Override
    public String toString() {
        return content.toString();
    }

    private class ScopeTracker implements Closeable {
        @Override
        public void close() throws IOException {
            endScope();
        }
    }
}
