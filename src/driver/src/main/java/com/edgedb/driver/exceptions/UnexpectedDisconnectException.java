package com.edgedb.driver.exceptions;

/**
 * Represents an exception that was caused by an unexpected disconnection.
 */
public class UnexpectedDisconnectException extends GelException {
    /**
     * Constructs a new {@linkplain UnexpectedDisconnectException}.
     */
    public UnexpectedDisconnectException() {
        super("The connection was unexpectedly closed");
    }
}
