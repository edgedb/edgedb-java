package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import io.netty.buffer.ByteBuf;

public class ParameterStatus implements Receivable {
    public final String name;
    public final ByteBuf value;

    public ParameterStatus(PacketReader reader) {
        name = reader.readString();
        value = reader.readByteArray();
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.PARAMETER_STATUS;
    }
}
