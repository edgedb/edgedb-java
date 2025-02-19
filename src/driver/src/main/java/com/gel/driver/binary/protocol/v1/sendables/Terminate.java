package com.gel.driver.binary.protocol.v1.sendables;

import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.protocol.ClientMessageType;
import com.gel.driver.binary.protocol.Sendable;

public class Terminate extends Sendable {
    public static final Sendable INSTANCE = new Terminate();

    public Terminate() {
        super(ClientMessageType.TERMINATE);
    }

    @Override
    public int getDataSize() {
        return 0;
    }

    @Override
    protected void buildPacket(PacketWriter writer) {/* no data */ }
}
