package com.edgedb.driver.exceptions;

/**
 * Represents an exception that occurs when the binding doesn't have a codec for incoming or outgoing data, or the
 * binding can't serialize/deserialize certain data.
 */
public class MissingCodecException extends GelException {

    /**
     * Constructs a new {@linkplain MissingCodecException}.
     * @param message The error message describing why this exception was thrown.
     */
    public MissingCodecException(String message) {
        super(message);
    }
}
