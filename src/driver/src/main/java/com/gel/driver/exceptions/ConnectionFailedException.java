package com.gel.driver.exceptions;

/**
 * Represents a connection failure that cannot be retried.
 */
public class ConnectionFailedException extends GelException {
    /**
     * Constructs a new {@linkplain ConnectionFailedException}.
     * @param attempts The number of attempts the binding made to connect.
     */
    public ConnectionFailedException(int attempts) {
        super("The connection failed to be established after " + attempts + " attempt(s)", false, false);
    }

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

    /**
     * Constructs a new {@linkplain ConnectionFailedException}.
     * @param message The detailed message containing why this exception was thrown.
     */
    public ConnectionFailedException(String message) {
        super(message);
    }
}
