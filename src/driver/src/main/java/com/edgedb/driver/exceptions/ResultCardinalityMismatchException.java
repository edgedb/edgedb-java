package com.edgedb.driver.exceptions;

/**
 * Represents an exception that occurs when a queries cardinality isn't what the binding was expecting it to be.
 */
public final class ResultCardinalityMismatchException extends EdgeDBException {

    /**
     * Constructs a new {@linkplain ResultCardinalityMismatchException}.
     * @param expected The expected cardinality.
     * @param actual The actual cardinality.
     */
    public ResultCardinalityMismatchException(Enum<?> expected, Enum<?> actual) {
        super(String.format("Got mismatch on cardinality of query. Expected \"%s\" but got \"%s\"", expected, actual));
    }
}
