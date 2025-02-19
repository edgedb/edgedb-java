package com.gel.driver.binary.protocol;

import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.SerializableData;
import com.gel.driver.binary.protocol.ClientMessageType;
import org.jetbrains.annotations.NotNull;

import javax.naming.OperationNotSupportedException;

import static com.gel.driver.util.BinaryProtocolUtils.BYTE_SIZE;
import static com.gel.driver.util.BinaryProtocolUtils.INT_SIZE;

public abstract class Sendable implements SerializableData {
    public final ClientMessageType type;

    public Sendable(ClientMessageType type) {
        this.type = type;
    }

    protected abstract void buildPacket(final PacketWriter writer) throws OperationNotSupportedException;

    @Override
    public void write(final @NotNull PacketWriter writer) throws OperationNotSupportedException {
        writer.write(type.getCode());
        writer.write(getDataSize() + 4);
        buildPacket(writer);
    }

    @Override
    public int getSize() {
        return getDataSize() + BYTE_SIZE + INT_SIZE;
    }

    protected abstract int getDataSize();

}
