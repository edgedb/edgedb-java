package com.edgedb.driver.binary.packets.sendables;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.ClientMessageType;
import com.edgedb.driver.binary.packets.shared.Annotation;
import com.edgedb.driver.util.BinaryProtocolUtils;
import org.jetbrains.annotations.NotNull;

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
    protected void buildPacket(@NotNull PacketWriter writer) throws OperationNotSupportedException {
        writer.writeArray(this.annotations, Short.TYPE);
    }
}
