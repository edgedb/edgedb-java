package com.edgedb.driver.exceptions;

public class ScramException extends EdgeDBException {

    public ScramException() {
        super("Received malformed scram message");
    }

    public ScramException(String message) {
        super(message);
    }

    public ScramException(Exception inner) {
        super("Received malformed scram message", inner);
    }
}
