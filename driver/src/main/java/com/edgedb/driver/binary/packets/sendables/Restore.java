package com.edgedb.driver.binary.packets.sendables;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.ClientMessageType;
import com.edgedb.driver.binary.packets.shared.KeyValue;
import io.netty.buffer.ByteBuf;

import javax.naming.OperationNotSupportedException;

import static com.edgedb.driver.util.BinaryProtocolUtils.SHORT_SIZE;
import static com.edgedb.driver.util.BinaryProtocolUtils.sizeOf;

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
        return SHORT_SIZE + sizeOf(attributes, Short.TYPE) + sizeOf(headerData);
    }

    @Override
    protected void buildPacket(PacketWriter writer) throws OperationNotSupportedException {
        writer.writeArray(attributes, Short.TYPE);
        writer.write(jobs);
        writer.writeArray(headerData);
    }
}
