package com.edgedb.driver;

/**
 * Represents a generic transaction.
 */
public interface Transaction extends GelQueryable {
    /**
     * Gets the state of the current transaction.
     * @return The current transaction state.
     */
    TransactionState getState();
}
