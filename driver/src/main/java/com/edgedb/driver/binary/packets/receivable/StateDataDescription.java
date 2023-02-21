package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class StateDataDescription implements Receivable {
    public final UUID typeDescriptorId;
    public final ByteBuf typeDescriptorBuffer;

    public StateDataDescription(PacketReader reader) {
        typeDescriptorId = reader.readUUID();
        typeDescriptorBuffer = reader.readByteArray();
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.STATE_DATA_DESCRIPTION;
    }
}
