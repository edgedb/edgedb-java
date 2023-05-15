package com.edgedb.driver.binary.packets.shared;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.SerializableData;
import com.edgedb.driver.util.BinaryProtocolUtils;

import javax.naming.OperationNotSupportedException;

public class Annotation implements SerializableData {
    private final String name;
    private final String value;

    public Annotation(PacketReader reader) {
        name = reader.readString();
        value = reader.readString();
    }

    public Annotation(String name, String value) {
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
