package com.edgedb.driver.exceptions;

public class UnexpectedDisconnectException extends EdgeDBException {

    public UnexpectedDisconnectException() {
        super("The connection was unexpectedly closed");
    }
}
