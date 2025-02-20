package com.gel.driver.exceptions;

/**
 * Represents a generic exception that occurred within the binding.
 */
public class GelException extends Exception {
    public final boolean shouldRetry;
    public final boolean shouldReconnect;

    /**
     * Constructs a new {@linkplain GelException}.
     * @param shouldRetry Whether the binding should retry the current operation, following the configuration rules
     *                    of retryable errors.
     * @param shouldReconnect Whether the binding should clear its state and reconnect to Gel.
     */
    public GelException(boolean shouldRetry, boolean shouldReconnect) {
        this.shouldRetry = shouldRetry;
        this.shouldReconnect = shouldReconnect;
    }

    /**
     * Constructs a new {@linkplain GelException}.
     * @param message The error message describing why this exception was thrown.
     * @param shouldRetry Whether the binding should retry the current operation, following the configuration rules
     *                    of retryable errors.
     * @param shouldReconnect Whether the binding should clear its state and reconnect to Gel.
     */
    public GelException(String message, boolean shouldRetry, boolean shouldReconnect) {
        super(message);

        this.shouldRetry = shouldRetry;
        this.shouldReconnect = shouldReconnect;
    }

    /**
     * Constructs a new {@linkplain GelException}.
     * @param message The error message describing why this exception was thrown.
     * @param inner The inner cause of this exception.
     * @param shouldRetry Whether the binding should retry the current operation, following the configuration rules
     *                    of retryable errors.
     * @param shouldReconnect Whether the binding should clear its state and reconnect to Gel.
     */
    public GelException(String message, Throwable inner, boolean shouldRetry, boolean shouldReconnect) {
        super(message, inner);

        this.shouldRetry = shouldRetry;
        this.shouldReconnect = shouldReconnect;
    }

    /**
     * Constructs a new {@linkplain GelException}.
     * @param message The error message describing why this exception was thrown.
     */
    public GelException(String message) {
        super(message);

        this.shouldRetry = false;
        this.shouldReconnect = false;
    }

    /**
     * Constructs a new {@linkplain GelException}.
     * @param message The error message describing why this exception was thrown.
     * @param inner The inner cause of this exception.
     */
    public GelException(String message, Throwable inner) {
        super(message, inner);

        this.shouldRetry = false;
        this.shouldReconnect = false;
    }
}
