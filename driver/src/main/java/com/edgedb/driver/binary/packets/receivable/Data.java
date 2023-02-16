package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;

import java.nio.ByteBuffer;

public class Data implements Receivable {
    public final ByteBuffer payloadBuffer;

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
