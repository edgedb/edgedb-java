package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
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
