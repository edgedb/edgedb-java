package com.edgedb.driver;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.SerializableData;

import javax.naming.OperationNotSupportedException;

public enum Capabilities implements SerializableData {
    READ_ONLY         (0),
    MODIFICATIONS     (1),
    SESSION_CONFIG    (1 << 1),
    TRANSACTION       (1 << 2),
    DDL               (1 << 3),
    PERSISTENT_CONFIG(1 << 4),
    ALL               (0xffffffffffffffffL);

    private final long flags;
    Capabilities(long value) {
        this.flags = value;
    }

    public long getFlags() {
        return this.flags;
    }

    @Override
    public void write(PacketWriter writer) throws OperationNotSupportedException {

    }

    @Override
    public int getSize() {
        return 0;
    }
}
