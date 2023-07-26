package com.edgedb.driver.binary.protocol.v1.receivables;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.protocol.Receivable;
import com.edgedb.driver.binary.protocol.ServerMessageType;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

public class ServerKeyData implements Receivable {
    public static final int SERVER_KEY_LENGTH = 32;

    public final ByteBuf keyData;

    public ServerKeyData(@NotNull PacketReader reader) {
        keyData = reader.readBytes(SERVER_KEY_LENGTH);
    }

    @Override
    public void close() {
        if(keyData != null) {
            keyData.release();
        }
    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.SERVER_KEY_DATA;
    }
}
