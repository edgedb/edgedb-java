package com.edgedb.driver.exceptions;

public final class ResultCardinalityMismatchException extends EdgeDBException {
    public ResultCardinalityMismatchException(Enum<?> expected, Enum<?> actual) {
        super(String.format("Got mismatch on cardinality of query. Expected \"%s\" but got \"%s\"", expected, actual));
    }
}
