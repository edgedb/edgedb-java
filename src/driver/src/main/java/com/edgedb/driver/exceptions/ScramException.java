package com.edgedb.driver.exceptions;

/**
 * Represents an exception thrown because of a malformed scram message.
 */
public class ScramException extends EdgeDBException {
    /**
     * Constructs a new {@linkplain ScramException}.
     */
    public ScramException() {
        super("Received malformed scram message");
    }

    /**
     * Constructs a new {@linkplain ScramException}.
     * @param message The message describing why this exception was thrown.
     */
    public ScramException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@linkplain ScramException}.
     * @param inner The inner cause of this exception.
     */
    public ScramException(Exception inner) {
        super("Received malformed scram message", inner);
    }
}
