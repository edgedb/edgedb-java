package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.AuthStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public class AuthenticationStatus implements Receivable {
    public final AuthStatus authStatus;
    @Nullable
    public final String[] authenticationMethods;
    @Nullable
    public final ByteBuffer saslData;

    public AuthenticationStatus(PacketReader reader) {
        this.authStatus = AuthStatus.valueOf(reader.readInt32());

        switch (this.authStatus) {
            case AUTHENTICATION_REQUIRED_SASL_MESSAGE:
                authenticationMethods = reader.readStringArray();
                saslData = null;
                break;
            case AUTHENTICATION_SASL_CONTINUE:
            case AUTHENTICATION_SASL_FINAL:
                saslData = reader.readByteArray();
                authenticationMethods = null;
                break;
            default:
                authenticationMethods = null;
                saslData = null;

        }
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.AUTHENTICATION;
    }
}
