package com.edgedb.driver;

import com.edgedb.driver.binary.BinaryEnum;

/**
 * Represents the error severity for an error returned by EdgeDB
 * @see com.edgedb.driver.exceptions.EdgeDBErrorException
 */
public enum ErrorSeverity implements BinaryEnum<Byte> {
    ERROR (0x78),
    FATAL (0xC8),
    PANIC (0xFF);

    private final byte value;

    ErrorSeverity(int value) {
        this.value = (byte)value;
    }

    @Override
    public Byte getValue() {
        return value;
    }
}
