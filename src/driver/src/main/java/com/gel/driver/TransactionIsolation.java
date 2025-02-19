package com.gel.driver;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the different isolation modes that can be used within a transaction.
 */
public enum TransactionIsolation {
    /**
     * All statements of the current transaction can only see data changes committed before the first query or
     * data-modification statement was executed in this transaction. If a pattern of reads and writes among concurrent
     * serializable transactions would create a situation which could not have occurred for any serial (one-at-a-time)
     * execution of those transactions, one of them will be rolled back with a serialization_failure error.
     */
    SERIALIZABLE,

    /**
     * All statements of the current transaction can only see data committed before the first query or data-modification
     * statement was executed in this transaction.
     * @deprecated in EdgeDB >=1.3
     */
    @Deprecated
    REPEATABLE_READ;

    @Override
    public @NotNull String toString() {
        if(this == SERIALIZABLE) {
            return "serializable";
        }

        throw new RuntimeException("Unknown or unsupported isolation mode " + this.name());
    }
}
