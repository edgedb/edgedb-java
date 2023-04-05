package com.edgedb.binary.packets.sendables;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.packets.ClientMessageType;
import com.edgedb.binary.packets.shared.KeyValue;
import com.edgedb.util.BinaryProtocolUtils;
import io.netty.buffer.ByteBuf;

import javax.naming.OperationNotSupportedException;

import static com.edgedb.util.BinaryProtocolUtils.sizeOf;

public class Restore extends Sendable{
    private final KeyValue[] attributes;
    private final short jobs;
    private final ByteBuf headerData;

    public Restore(KeyValue[] attributes, short jobs, ByteBuf headerData) {
        super(ClientMessageType.RESTORE);
        this.attributes = attributes;
        this.jobs = jobs;
        this.headerData = headerData;
    }

    @Override
    public int getDataSize() {
        return BinaryProtocolUtils.SHORT_SIZE + BinaryProtocolUtils.sizeOf(attributes, Short.TYPE) + BinaryProtocolUtils.sizeOf(headerData);
    }

    @Override
    protected void buildPacket(PacketWriter writer) throws OperationNotSupportedException {
        writer.writeArray(attributes, Short.TYPE);
        writer.write(jobs);
        writer.writeArray(headerData);
    }
}
