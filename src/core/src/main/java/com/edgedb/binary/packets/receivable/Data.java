package com.edgedb.binary.packets.receivable;

import com.edgedb.binary.PacketReader;
import com.edgedb.binary.packets.ServerMessageType;
import io.netty.buffer.ByteBuf;

public class Data implements Receivable {
    public final ByteBuf payloadBuffer;

    public Data(PacketReader reader) {
        var numElements = reader.readInt16();

        if(numElements != 1) {
            throw new IndexOutOfBoundsException("Expected one element array for data, got " + numElements);
        }

        payloadBuffer = reader.readByteArray();
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.DATA;
    }
}
