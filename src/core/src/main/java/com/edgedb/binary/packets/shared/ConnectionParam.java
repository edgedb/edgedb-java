package com.edgedb.binary.packets.shared;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.SerializableData;
import com.edgedb.util.BinaryProtocolUtils;

import javax.naming.OperationNotSupportedException;

public class ConnectionParam implements SerializableData {
    private final String name;
    private final String value;

    public ConnectionParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void write(PacketWriter writer) throws OperationNotSupportedException {
        writer.write(this.name);
        writer.write(this.value);
    }

    @Override
    public int getSize() {
        return BinaryProtocolUtils.sizeOf(name) + BinaryProtocolUtils.sizeOf(value);
    }
}
