package com.gel.driver.binary.protocol.v1.receivables;

import com.gel.driver.binary.protocol.Receivable;
import com.gel.driver.binary.protocol.common.ProtocolExtension;
import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.ServerMessageType;
import org.jetbrains.annotations.NotNull;
import org.joou.UShort;

public class ServerHandshake implements Receivable {
    public final @NotNull UShort majorVersion;
    public final @NotNull UShort minorVersion;
    public final ProtocolExtension @NotNull [] extensions;

    public ServerHandshake(@NotNull PacketReader reader) {
        majorVersion = reader.readUInt16();
        minorVersion = reader.readUInt16();
        extensions = reader.readArrayOf(ProtocolExtension.class, ProtocolExtension::new, Short.TYPE);
    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.SERVER_HANDSHAKE;
    }
}
