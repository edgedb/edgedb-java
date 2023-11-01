package com.edgedb.driver.binary.protocol.v1.sendables;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.protocol.ClientMessageType;
import com.edgedb.driver.binary.protocol.Sendable;
import com.edgedb.driver.util.BinaryProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import javax.naming.OperationNotSupportedException;

public class AuthenticationSASLResponse extends Sendable {
    private final ByteBuf payload;

    public AuthenticationSASLResponse(ByteBuf payload) {
        super(ClientMessageType.AUTHENTICATION_SASL_RESPONSE);
        this.payload = payload;
    }

    @Override
    protected void buildPacket(@NotNull PacketWriter writer) throws OperationNotSupportedException {
        writer.writeArray(payload);
    }

    @Override
    public int getDataSize() {
        return BinaryProtocolUtils.sizeOf(this.payload);
    }
}
