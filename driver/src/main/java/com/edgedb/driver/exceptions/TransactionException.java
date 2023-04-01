package com.edgedb.driver.exceptions;

public final class TransactionException extends EdgeDBException {
    public TransactionException(String err) {
        super(err, false, false);
    }

    public TransactionException(String err, Throwable inner) {
        super(err, inner);
    }
}
