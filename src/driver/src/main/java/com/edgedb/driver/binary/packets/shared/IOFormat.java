package com.edgedb.driver.binary.packets.shared;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.BinaryEnum;
import com.edgedb.driver.binary.SerializableData;
import com.edgedb.driver.util.BinaryProtocolUtils;

import javax.naming.OperationNotSupportedException;

public enum IOFormat implements SerializableData, BinaryEnum<Byte> {
    BINARY        (0x62),
    JSON          (0x6a),
    JSON_ELEMENTS (0x4a),
    NONE          (0x6e);

    private final byte value;
    IOFormat(int value) {
        this.value = (byte)value;
    }

    @Override
    public void write(PacketWriter writer) throws OperationNotSupportedException {
        writer.write(this.value);
    }

    @Override
    public int getSize() {
        return BinaryProtocolUtils.BYTE_SIZE;
    }

    @Override
    public Byte getValue() {
        return this.value;
    }
}
