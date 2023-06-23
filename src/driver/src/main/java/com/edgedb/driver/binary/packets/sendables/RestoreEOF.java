package com.edgedb.driver.binary.packets.sendables;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.ClientMessageType;

public class RestoreEOF extends Sendable{
    public RestoreEOF() {
        super(ClientMessageType.RESTORE_EOF);
    }

    @Override
    public int getDataSize() {
        return 0;
    }

    @Override
    protected void buildPacket(PacketWriter writer) { /* no data */ }
}
