package com.edgedb.driver.binary.packets.sendables;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.ClientMessageType;
import com.edgedb.driver.binary.packets.shared.ConnectionParam;
import com.edgedb.driver.binary.packets.shared.ProtocolExtension;

import javax.naming.OperationNotSupportedException;

import static com.edgedb.driver.util.BinaryProtocolUtils.*;

public class ClientHandshake extends Sendable {
    private final short majorVersion;
    private final short minorVersion;
    private final ConnectionParam[] connectionParams;
    private final ProtocolExtension[] extensions;

    public ClientHandshake(
            short majorVersion,
            short minorVersion,
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
