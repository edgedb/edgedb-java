package com.edgedb.binary.packets.sendables;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.packets.ClientMessageType;
import com.edgedb.binary.packets.shared.Annotation;
import com.edgedb.util.BinaryProtocolUtils;

import javax.naming.OperationNotSupportedException;

public class Dump extends Sendable {
    private final Annotation[] annotations;

    public Dump(Annotation[] annotations) {
        super(ClientMessageType.DUMP);
        this.annotations = annotations;
    }

    @Override
    public int getDataSize() {
        return BinaryProtocolUtils.sizeOf(this.annotations, Short.TYPE);
    }

    @Override
    protected void buildPacket(PacketWriter writer) throws OperationNotSupportedException {
        writer.writeArray(this.annotations, Short.TYPE);
    }
}
