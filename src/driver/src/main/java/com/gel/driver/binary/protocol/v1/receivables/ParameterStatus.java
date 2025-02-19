package com.gel.driver.binary.protocol.v1.receivables;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.Receivable;
import com.gel.driver.binary.protocol.ServerMessageType;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParameterStatus implements Receivable {
    public final @NotNull String name;
    public final @Nullable ByteBuf value;

    public ParameterStatus(@NotNull PacketReader reader) {
        name = reader.readString();
        value = reader.readByteArray();
    }

    @Override
    public void close() {
        if(value != null) {
            value.release();
        }
    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.PARAMETER_STATUS;
    }
}
