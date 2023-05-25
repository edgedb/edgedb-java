package com.edgedb.driver;

/**
 * Represents a generic transaction.
 */
public interface Transaction extends EdgeDBQueryable {
    /**
     * Gets the state of the current transaction.
     * @return The current transaction state.
     */
    TransactionState getState();
}
