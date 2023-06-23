package com.edgedb.driver.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an exception that occurs when the binding cannot find a way to deserialize a given type.
 */
public class NoTypeConverterException extends EdgeDBException {

    /**
     * Constructs a new {@linkplain NoTypeConverterException}.
     * @param target The target type that couldn't be converted.
     */
    public NoTypeConverterException(@NotNull Class<?> target) {
        this(String.format("No type converter found for type %s", target.getName()));
    }

    /**
     * Constructs a new {@linkplain NoTypeConverterException}.
     * @param message The detailed message describing why the exception was thrown.
     */
    public NoTypeConverterException(String message) {
        super(message);
    }
}
