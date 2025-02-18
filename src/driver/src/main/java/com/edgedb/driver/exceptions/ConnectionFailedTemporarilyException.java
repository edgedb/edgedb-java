package com.edgedb.driver.exceptions;

/**
 * Represents a temporary connection failure exception.
 */
public class ConnectionFailedTemporarilyException extends GelException {

    /**
     * Constructs a new {@linkplain ConnectionFailedTemporarilyException}.
     * @param err The inner cause of the connection failure.
     */
    public ConnectionFailedTemporarilyException(Throwable err) {
        super("The connection could not be established at this time", err, true, true);
    }

    /**
     * Constructs a new {@linkplain ConnectionFailedTemporarilyException}.
     * @param message A detailed message describing why this exception was raised.
     */
    public ConnectionFailedTemporarilyException(String message) {
        super(message);
    }
}
