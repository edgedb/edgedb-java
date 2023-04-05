package com.edgedb.binary.packets.sendables;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.packets.ClientMessageType;
import com.edgedb.util.BinaryProtocolUtils;
import io.netty.buffer.ByteBuf;

import javax.naming.OperationNotSupportedException;

public class AuthenticationSASLResponse extends Sendable {
    private final ByteBuf payload;

    public AuthenticationSASLResponse(ByteBuf payload) {
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
