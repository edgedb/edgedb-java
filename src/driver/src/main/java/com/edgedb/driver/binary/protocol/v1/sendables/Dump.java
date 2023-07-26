package com.edgedb.driver.binary.protocol.v1.sendables;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.protocol.ClientMessageType;
import com.edgedb.driver.binary.protocol.Sendable;
import com.edgedb.driver.binary.protocol.common.Annotation;
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
