package com.edgedb.driver.binary.packets.shared;

import com.edgedb.driver.binary.BinaryEnum;

public enum CompilationFlags implements BinaryEnum<Long> {
    NONE                (0),
    IMPLICIT_TYPE_IDS   (1),
    IMPLICIT_TYPE_NAMES (1 << 1),
    EXPLICIT_OBJECT_IDS (1 << 2);

    private final long value;

    CompilationFlags(long value) {
        this.value = value;
    }

    @Override
    public Long getValue() {
        return this.value;
    }
}
