package com.edgedb.driver.binary.packets.sendables;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.ClientMessageType;
import com.edgedb.driver.binary.packets.shared.Annotation;
import com.edgedb.driver.util.BinaryProtocolUtils;

import javax.naming.OperationNotSupportedException;

public class Dump extends Sendable {
    private final Annotation[] annotations;

    public Dump(Annotation[] annotations) {
        super(ClientMessageType.DUMP);
        this.annotations = annotations;
    }

    @Override
    public int getSize() {
        return BinaryProtocolUtils.sizeOf(this.annotations);
    }

    @Override
    protected void buildPacket(PacketWriter writer) throws OperationNotSupportedException {
        writer.writeArray(this.annotations);
    }
}
