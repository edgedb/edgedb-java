package com.edgedb.driver.binary.packets.sendables;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.ClientMessageType;
import com.edgedb.driver.binary.packets.SerializableData;

import javax.naming.OperationNotSupportedException;

public abstract class Sendable implements SerializableData {
    public final ClientMessageType type;

    public Sendable(ClientMessageType type) {
        this.type = type;
    }

    protected abstract void buildPacket(final PacketWriter writer) throws OperationNotSupportedException;

    @Override
    public void write(final PacketWriter writer) throws OperationNotSupportedException {
        writer.write(getSize() + 4);
        buildPacket(writer);
    }
}
