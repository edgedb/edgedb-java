package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.KeyValue;

public class DumpBlock implements Receivable {
    public final KeyValue[] attributes;

    public DumpBlock(PacketReader reader) {
        attributes = reader.readAttributes();
    }

    @Override
    public void close() throws Exception {
        release(attributes);
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.DUMP_BLOCK;
    }
}
