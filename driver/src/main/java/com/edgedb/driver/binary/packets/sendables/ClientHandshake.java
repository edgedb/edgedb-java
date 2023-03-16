package com.edgedb.driver.binary.packets.sendables;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.ClientMessageType;
import com.edgedb.driver.binary.packets.shared.ConnectionParam;
import com.edgedb.driver.binary.packets.shared.ProtocolExtension;
import org.joou.UShort;

import javax.naming.OperationNotSupportedException;

import static com.edgedb.driver.util.BinaryProtocolUtils.SHORT_SIZE;
import static com.edgedb.driver.util.BinaryProtocolUtils.sizeOf;

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
    protected void buildPacket(PacketWriter writer) throws OperationNotSupportedException {
        writer.write(this.majorVersion);
        writer.write(this.minorVersion);
        writer.writeArray(this.connectionParams, Short.TYPE);
        writer.writeArray(this.extensions, Short.TYPE);
    }

    @Override
    public int getDataSize() {
        return
                SHORT_SIZE +
                SHORT_SIZE +
                sizeOf(connectionParams, Short.TYPE) +
                sizeOf(extensions, Short.TYPE);
    }
}
