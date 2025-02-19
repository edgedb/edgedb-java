package com.gel.driver.binary.protocol.common;

import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.SerializableData;
import com.gel.driver.util.BinaryProtocolUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public class ConnectionParam implements SerializableData {
    private final String name;
    private final @Nullable String value;

    public ConnectionParam(String name, @Nullable String value) {
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
