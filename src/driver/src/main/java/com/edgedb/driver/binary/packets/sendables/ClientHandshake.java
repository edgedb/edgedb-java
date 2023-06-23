package com.edgedb.driver.binary.packets.sendables;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.shared.ConnectionParam;
import com.edgedb.driver.binary.packets.shared.ProtocolExtension;
import com.edgedb.driver.binary.packets.ClientMessageType;
import com.edgedb.driver.util.BinaryProtocolUtils;
import org.jetbrains.annotations.NotNull;
import org.joou.UShort;

import javax.naming.OperationNotSupportedException;

public class ClientHandshake extends Sendable {
    private final UShort majorVersion;
    private final UShort minorVersion;
    private final ConnectionParam[] connectionParams;
    private final ProtocolExtension[] extensions;

    public ClientHandshake(
            UShort majorVersion,
            UShort minorVersion,
            ConnectionParam[] connectionParams,
            ProtocolExtension[] extensions
    ) {
        super(ClientMessageType.CLIENT_HANDSHAKE);
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.connectionParams = connectionParams;
        this.extensions = extensions;
    }

    @Override
    protected void buildPacket(@NotNull PacketWriter writer) throws OperationNotSupportedException {
        writer.write(this.majorVersion);
        writer.write(this.minorVersion);
        writer.writeArray(this.connectionParams, Short.TYPE);
        writer.writeArray(this.extensions, Short.TYPE);
    }

    @Override
    public int getDataSize() {
        return
                BinaryProtocolUtils.SHORT_SIZE +
                BinaryProtocolUtils.SHORT_SIZE +
                BinaryProtocolUtils.sizeOf(connectionParams, Short.TYPE) +
                BinaryProtocolUtils.sizeOf(extensions, Short.TYPE);
    }
}
