package com.gel.driver.binary.protocol.common;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.SerializableData;
import org.jetbrains.annotations.NotNull;

import javax.naming.OperationNotSupportedException;

public class ProtocolExtension implements SerializableData {
    private final String name;
    private final Annotation[] annotations;

    public ProtocolExtension(String name, Annotation[] annotations) {
        this.name = name;
        this.annotations = annotations;
    }

    public ProtocolExtension(@NotNull PacketReader reader) {
        this.name = reader.readString();
        this.annotations = reader.readAnnotations();
    }

    @Override
    public void write(@NotNull PacketWriter writer) throws OperationNotSupportedException {
        writer.write(this.name);
        writer.writeArray(this.annotations, Short.TYPE);
    }

    @Override
    public int getSize() {
        return 0;
    }
}
