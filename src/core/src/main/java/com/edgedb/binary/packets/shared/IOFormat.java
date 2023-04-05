package com.edgedb.binary.packets.shared;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.BinaryEnum;
import com.edgedb.binary.SerializableData;
import com.edgedb.util.BinaryProtocolUtils;

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
