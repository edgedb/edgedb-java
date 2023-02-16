package com.edgedb.driver.binary.packets.sendables;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.ClientMessageType;
import com.edgedb.driver.util.BinaryProtocolUtils;

import javax.naming.OperationNotSupportedException;
import java.nio.ByteBuffer;

public class RestoreBlock extends Sendable {
    private final ByteBuffer blockData;

    public RestoreBlock(ByteBuffer blockData) {
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
