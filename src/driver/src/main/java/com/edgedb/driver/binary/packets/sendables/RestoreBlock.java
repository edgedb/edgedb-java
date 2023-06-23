package com.edgedb.driver.binary.packets.sendables;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.ClientMessageType;
import com.edgedb.driver.util.BinaryProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

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
    protected void buildPacket(@NotNull PacketWriter writer) throws OperationNotSupportedException {
        writer.writeArray(blockData);
    }
}
