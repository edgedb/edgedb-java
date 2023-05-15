package com.edgedb.driver.exceptions;

public class ConnectionFailedTemporarilyException extends EdgeDBException {
    public ConnectionFailedTemporarilyException(Throwable err) {
        super("The connection could not be established at this time", err, true, true);
    }
}
