package com.gel.driver.binary.protocol.v1.receivables;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.Receivable;
import com.gel.driver.binary.protocol.ServerMessageType;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Data implements Receivable {
    public final @Nullable ByteBuf payloadBuffer;

    public Data(@NotNull PacketReader reader) {
        var numElements = reader.readInt16();

        if(numElements != 1) {
            throw new IndexOutOfBoundsException("Expected one element array for data, got " + numElements);
        }

        payloadBuffer = reader.readByteArray();
    }

    @Override
    public void close() {
        if(payloadBuffer != null) {
            payloadBuffer.release();
        }
    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.DATA;
    }
}
