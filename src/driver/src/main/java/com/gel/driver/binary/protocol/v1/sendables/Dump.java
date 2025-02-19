package com.gel.driver.binary.protocol.v1.sendables;

import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.protocol.ClientMessageType;
import com.gel.driver.binary.protocol.Sendable;
import com.gel.driver.binary.protocol.common.Annotation;
import com.gel.driver.util.BinaryProtocolUtils;
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
