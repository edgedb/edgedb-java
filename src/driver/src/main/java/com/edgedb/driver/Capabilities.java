package com.edgedb.driver;

import com.edgedb.driver.binary.BinaryEnum;
import org.jetbrains.annotations.NotNull;

public enum Capabilities implements BinaryEnum<Long> {
    READ_ONLY         (0),
    MODIFICATIONS     (1),
    SESSION_CONFIG    (1 << 1),
    TRANSACTION       (1 << 2),
    DDL               (1 << 3),
    PERSISTENT_CONFIG(1 << 4),
    ALL               (0xffffffffffffffffL);

    private final long value;

    Capabilities(long value) {
        this.value = value;
    }
    @Override
    public @NotNull Long getValue() {
        return this.value;
    }
}
