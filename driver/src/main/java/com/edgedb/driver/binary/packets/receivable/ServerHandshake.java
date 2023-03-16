package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.ProtocolExtension;
import org.joou.UShort;

public class ServerHandshake implements Receivable {
    public final UShort majorVersion;
    public final UShort minorVersion;
    public final ProtocolExtension[] extensions;

    public ServerHandshake(PacketReader reader) {
        majorVersion = reader.readUInt16();
        minorVersion = reader.readUInt16();
        extensions = reader.readArrayOf(ProtocolExtension.class, ProtocolExtension::new, Short.TYPE);
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.SERVER_HANDSHAKE;
    }
}
