package com.edgedb.driver.exceptions;

/**
 * Represents an exception caused by an unexpected message.
 */
public class UnexpectedMessageException extends EdgeDBException {
    /**
     * Constructs a new {@linkplain UnexpectedMessageException}.
     * @param message The error message describing why this exception was thrown.
     */
    public UnexpectedMessageException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@linkplain UnexpectedMessageException}.
     * @param unexpected The message type that wasn't expected.
     */
    public UnexpectedMessageException(Enum<?> unexpected) {
        super(String.format("Got unexpected message type %s", unexpected));
    }

    /**
     * Constructs anew {@linkplain UnexpectedMessageException}.
     * @param expected The expected message type.
     * @param actual The actual message type.
     */
    public UnexpectedMessageException(Enum<?> expected, Enum<?> actual) {
        super(String.format("Expected message type %s but got %s", expected, actual));
    }
}
