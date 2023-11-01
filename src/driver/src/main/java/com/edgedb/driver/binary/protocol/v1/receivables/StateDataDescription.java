package com.edgedb.driver.binary.protocol.v1.receivables;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.protocol.Receivable;
import com.edgedb.driver.binary.protocol.ServerMessageType;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class StateDataDescription implements Receivable {
    public final @NotNull UUID typeDescriptorId;
    public final @Nullable ByteBuf typeDescriptorBuffer;

    public StateDataDescription(@NotNull PacketReader reader) {
        typeDescriptorId = reader.readUUID();
        typeDescriptorBuffer = reader.readByteArray();
    }

    @Override
    public void close() {
        if(typeDescriptorBuffer != null) {
            typeDescriptorBuffer.release();
        }
    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.STATE_DATA_DESCRIPTION;
    }
}
