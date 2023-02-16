package com.edgedb.driver.binary.packets.sendables;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.ClientMessageType;
import com.edgedb.driver.util.BinaryProtocolUtils;

import javax.naming.OperationNotSupportedException;
import java.nio.ByteBuffer;

public class AuthenticationSASLResponse extends Sendable {
    private final ByteBuffer payload;

    public AuthenticationSASLResponse(ByteBuffer payload) {
        super(ClientMessageType.AUTHENTICATION_SASL_RESPONSE);
        this.payload = payload;
    }

    @Override
    protected void buildPacket(PacketWriter writer) throws OperationNotSupportedException {
        writer.writeArray(payload);
    }

    @Override
    public int getDataSize() {
        return BinaryProtocolUtils.sizeOf(this.payload);
    }
}
