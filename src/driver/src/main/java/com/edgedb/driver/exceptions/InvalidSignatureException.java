package com.edgedb.driver.exceptions;

/**
 * Represents an exception caused by an invalid signature.
 */
public class InvalidSignatureException extends EdgeDBException {
    /**
     * Constructs a new {@linkplain InvalidSignatureException}.
     */
    public InvalidSignatureException() {
        super("The received signature didn't match the expected one");
    }
}
