package com.edgedb.binary.packets.sendables;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.packets.ClientMessageType;
import com.edgedb.util.BinaryProtocolUtils;
import io.netty.buffer.ByteBuf;

import javax.naming.OperationNotSupportedException;

public class RestoreBlock extends Sendable {
    private final ByteBuf blockData;

    public RestoreBlock(ByteBuf blockData) {
        super(ClientMessageType.RESTORE_BLOCK);
        this.blockData = blockData;
    }

    @Override
    public int getDataSize() {
        return BinaryProtocolUtils.sizeOf(blockData);
    }

    @Override
    protected void buildPacket(PacketWriter writer) throws OperationNotSupportedException {
        writer.writeArray(blockData);
    }
}
