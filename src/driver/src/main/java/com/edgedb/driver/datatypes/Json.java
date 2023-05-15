package com.edgedb.driver.datatypes;

import org.jetbrains.annotations.NotNull;

// TODO: flesh out
public final class Json {
    private final @NotNull String value;

    public Json(@NotNull String value) {
        this.value = value;
    }

    public @NotNull String getValue() {
        return this.value;
    }
}
