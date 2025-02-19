package com.gel.driver.binary.protocol.v1.sendables;

import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.protocol.ClientMessageType;
import com.gel.driver.binary.protocol.Sendable;

public class RestoreEOF extends Sendable {
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
