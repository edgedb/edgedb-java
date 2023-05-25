package com.edgedb.driver.exceptions;

/**
 * Represents a generic exception that occurred within the binding.
 */
public class EdgeDBException extends Exception {
    public final boolean shouldRetry;
    public final boolean shouldReconnect;

    /**
     * Constructs a new {@linkplain EdgeDBException}.
     * @param shouldRetry Whether the binding should retry the current operation, following the configuration rules
     *                    of retryable errors.
     * @param shouldReconnect Whether the binding should clear its state and reconnect to EdgeDB.
     */
    public EdgeDBException(boolean shouldRetry, boolean shouldReconnect) {
        this.shouldRetry = shouldRetry;
        this.shouldReconnect = shouldReconnect;
    }

    /**
     * Constructs a new {@linkplain EdgeDBException}.
     * @param message The error message describing why this exception was thrown.
     * @param shouldRetry Whether the binding should retry the current operation, following the configuration rules
     *                    of retryable errors.
     * @param shouldReconnect Whether the binding should clear its state and reconnect to EdgeDB.
     */
    public EdgeDBException(String message, boolean shouldRetry, boolean shouldReconnect) {
        super(message);

        this.shouldRetry = shouldRetry;
        this.shouldReconnect = shouldReconnect;
    }

    /**
     * Constructs a new {@linkplain EdgeDBException}.
     * @param message The error message describing why this exception was thrown.
     * @param inner The inner cause of this exception.
     * @param shouldRetry Whether the binding should retry the current operation, following the configuration rules
     *                    of retryable errors.
     * @param shouldReconnect Whether the binding should clear its state and reconnect to EdgeDB.
     */
    public EdgeDBException(String message, Throwable inner, boolean shouldRetry, boolean shouldReconnect) {
        super(message, inner);

        this.shouldRetry = shouldRetry;
        this.shouldReconnect = shouldReconnect;
    }

    /**
     * Constructs a new {@linkplain EdgeDBException}.
     * @param message The error message describing why this exception was thrown.
     */
    public EdgeDBException(String message) {
        super(message);

        this.shouldRetry = false;
        this.shouldReconnect = false;
    }

    /**
     * Constructs a new {@linkplain EdgeDBException}.
     * @param message The error message describing why this exception was thrown.
     * @param inner The inner cause of this exception.
     */
    public EdgeDBException(String message, Throwable inner) {
        super(message, inner);

        this.shouldRetry = false;
        this.shouldReconnect = false;
    }
}
