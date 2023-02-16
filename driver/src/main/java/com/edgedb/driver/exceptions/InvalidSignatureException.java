package com.edgedb.driver.exceptions;

public class InvalidSignatureException extends EdgeDBException {
    public InvalidSignatureException() {
        super("The received signature didn't match the expected one");
    }
}
