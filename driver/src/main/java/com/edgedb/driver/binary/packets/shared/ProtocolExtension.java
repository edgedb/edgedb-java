package com.edgedb.driver.binary.packets.shared;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.SerializableData;

import javax.naming.OperationNotSupportedException;

public class ProtocolExtension implements SerializableData {
    private final String name;
    private final Annotation[] annotations;

    public ProtocolExtension(String name, Annotation[] annotations) {
        this.name = name;
        this.annotations = annotations;
    }

    @Override
    public void write(PacketWriter writer) throws OperationNotSupportedException {
        writer.write(this.name);
        writer.writeArray(this.annotations);
    }

    @Override
    public int getSize() {
        return 0;
    }
}
