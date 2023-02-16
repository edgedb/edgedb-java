package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.ProtocolExtension;

public class ServerHandshake implements Receivable {
    public final short majorVersion;
    public final short minorVersion;
    public final ProtocolExtension[] extensions;

    public ServerHandshake(PacketReader reader) {
        majorVersion = reader.readInt16();
        minorVersion = reader.readInt16();
        extensions = reader.readArrayOf(ProtocolExtension.class, ProtocolExtension::new, Short.TYPE);
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.SERVER_HANDSHAKE;
    }
}
