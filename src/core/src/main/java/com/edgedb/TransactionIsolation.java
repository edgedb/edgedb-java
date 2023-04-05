package com.edgedb;

public enum TransactionIsolation {
    SERIALIZABLE,
    @Deprecated
    REPEATABLE_READ;

    @Override
    public String toString() {
        if(this == SERIALIZABLE) {
            return "serializable";
        }

        throw new RuntimeException("Unknown or unsupported isolation mode " + this.name());
    }
}
