package com.edgedb.driver.exceptions;

/**
 * Represents a connection failure that cannot be retried.
 */
public class ConnectionFailedException extends EdgeDBException {
    /**
     * Constructs a new {@linkplain ConnectionFailedException}.
     * @param attempts The number of attempts the binding made to connect.
     * @param cause The inner cause of this exception.
     */
    public ConnectionFailedException(int attempts, Throwable cause) {
        super("The connection failed to be established after " + attempts + " attempt(s)", cause, false, false);
    }

    /**
     * Constructs a new {@linkplain ConnectionFailedException}.
     * @param cause The inner cause of this exception.
     */
    public ConnectionFailedException(Throwable cause) {
        super("The connection failed to be established", cause, false, false);
    }
}
