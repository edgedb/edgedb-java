package com.edgedb.binary.packets.sendables;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.packets.ClientMessageType;
import com.edgedb.util.BinaryProtocolUtils;
import io.netty.buffer.ByteBuf;

import javax.naming.OperationNotSupportedException;

public class AuthenticationSASLInitialResponse extends Sendable {
    private final String method;
    private final ByteBuf payload;

    public AuthenticationSASLInitialResponse(ByteBuf payload, String method) {
        super(ClientMessageType.AUTHENTICATION_SASL_INITIAL_RESPONSE);
        this.method = method;
        this.payload = payload;
    }

    @Override
    protected void buildPacket(PacketWriter writer) throws OperationNotSupportedException {
        writer.write(method);
        writer.writeArray(payload);
    }

    @Override
    public int getDataSize() {
        return BinaryProtocolUtils.sizeOf(this.method) + BinaryProtocolUtils.sizeOf(this.payload);
    }
}
