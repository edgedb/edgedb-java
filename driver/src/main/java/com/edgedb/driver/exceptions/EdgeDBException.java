package com.edgedb.driver.exceptions;

public class EdgeDBException extends Exception {
    public final boolean shouldRetry;
    public final boolean shouldReconnect;

    public EdgeDBException(boolean shouldRetry, boolean shouldReconnect) {
        this.shouldRetry = shouldRetry;
        this.shouldReconnect = shouldReconnect;
    }

    public EdgeDBException(String message, boolean shouldRetry, boolean shouldReconnect) {
        super(message);

        this.shouldRetry = shouldRetry;
        this.shouldReconnect = shouldReconnect;
    }

    public EdgeDBException(String message, Exception inner, boolean shouldRetry, boolean shouldReconnect) {
        super(message, inner);

        this.shouldRetry = shouldRetry;
        this.shouldReconnect = shouldReconnect;
    }

    public EdgeDBException(String message) {
        super(message);

        this.shouldRetry = false;
        this.shouldReconnect = false;
    }
    public EdgeDBException(String message, Exception inner) {
        super(message, inner);

        this.shouldRetry = false;
        this.shouldReconnect = false;
    }
}
