package com.edgedb.exceptions;

public class UnexpectedDisconnectException extends EdgeDBException {

    public UnexpectedDisconnectException() {
        super("The connection was unexpectedly closed");
    }
}
