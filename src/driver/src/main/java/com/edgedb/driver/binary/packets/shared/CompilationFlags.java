package com.edgedb.driver.binary.packets.shared;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.BinaryEnum;
import com.edgedb.driver.binary.SerializableData;
import com.edgedb.driver.util.BinaryProtocolUtils;

import javax.naming.OperationNotSupportedException;

public enum CompilationFlags implements SerializableData, BinaryEnum<Long> {
    NONE                (0),
    IMPLICIT_TYPE_IDS   (1),
    IMPLICIT_TYPE_NAMES (1 << 1),
    EXPLICIT_OBJECT_IDS (1 << 2);

    private final long value;

    CompilationFlags(long value) {
        this.value = value;
    }

    @Override
    public void write(PacketWriter writer) throws OperationNotSupportedException {
        writer.write(this.value);
    }

    @Override
    public int getSize() {
        return BinaryProtocolUtils.LONG_SIZE;
    }

    @Override
    public Long getValue() {
        return this.value;
    }
}
