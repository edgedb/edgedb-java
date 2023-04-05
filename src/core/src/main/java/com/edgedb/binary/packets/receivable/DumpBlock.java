package com.edgedb.binary.packets.receivable;

import com.edgedb.binary.PacketReader;
import com.edgedb.binary.packets.ServerMessageType;
import com.edgedb.binary.packets.shared.KeyValue;

public class DumpBlock implements Receivable {
    public final KeyValue[] attributes;

    public DumpBlock(PacketReader reader) {
        attributes = reader.readAttributes();
    }


    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.DUMP_BLOCK;
    }
}
