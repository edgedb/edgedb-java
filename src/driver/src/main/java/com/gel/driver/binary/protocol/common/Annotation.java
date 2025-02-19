package com.gel.driver.binary.protocol.common;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.SerializableData;
import com.gel.driver.util.BinaryProtocolUtils;
import org.jetbrains.annotations.NotNull;

import javax.naming.OperationNotSupportedException;

public class Annotation implements SerializableData {
    private final String name;
    private final String value;

    public Annotation(@NotNull PacketReader reader) {
        name = reader.readString();
        value = reader.readString();
    }

    public Annotation(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void write(@NotNull PacketWriter writer) throws OperationNotSupportedException {
        writer.write(this.name);
        writer.write(this.value);
    }

    @Override
    public int getSize() {
        return BinaryProtocolUtils.sizeOf(name) + BinaryProtocolUtils.sizeOf(value);
    }
}
