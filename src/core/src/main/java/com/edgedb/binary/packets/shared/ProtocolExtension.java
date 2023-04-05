package com.edgedb.binary.packets.shared;

import com.edgedb.binary.PacketReader;
import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.SerializableData;

import javax.naming.OperationNotSupportedException;

public class ProtocolExtension implements SerializableData {
    private final String name;
    private final Annotation[] annotations;

    public ProtocolExtension(String name, Annotation[] annotations) {
        this.name = name;
        this.annotations = annotations;
    }

    public ProtocolExtension(PacketReader reader) {
        this.name = reader.readString();
        this.annotations = reader.readAnnotations();
    }

    @Override
    public void write(PacketWriter writer) throws OperationNotSupportedException {
        writer.write(this.name);
        writer.writeArray(this.annotations, Short.TYPE);
    }

    @Override
    public int getSize() {
        return 0;
    }
}
