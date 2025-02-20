package com.gel.driver.datatypes;

import org.jetbrains.annotations.NotNull;

/**
 * A class representing the {@code std::json} type in Gel.
 */
public final class Json {
    private final @NotNull String value;

    /**
     * Constructs a new {@linkplain Json} type.
     * @param value The raw json value.
     */
    public Json(@NotNull String value) {
        this.value = value;
    }

    /**
     * Gets the raw json value.
     * @return The raw json value.
     */
    public @NotNull String getValue() {
        return this.value;
    }
}
