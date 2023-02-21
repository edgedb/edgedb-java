package com.edgedb.driver.ssl;

import javax.net.ssl.SSLEngineResult;

public class HandshakeProcessResult {
    public final SSLEngineResult.HandshakeStatus operation;
    public final SSLEngineResult.Status processStatus;
    public final boolean hasErrored;
    public final String errorMessage;

    public final ChannelContext context;
    public final SSLEngineResult.HandshakeStatus handshakeStatus;

    private HandshakeProcessResult(
            SSLEngineResult.HandshakeStatus operation,
            SSLEngineResult.HandshakeStatus handshakeStatus,
            SSLEngineResult.Status processStatus,
            boolean hasErrored,
            String errorMessage,
            ChannelContext context) {
        this.operation = operation;
        this.handshakeStatus = context.getHandshakeStatus(handshakeStatus);
        this.processStatus = processStatus;
        this.hasErrored = hasErrored;
        this.errorMessage = errorMessage;
        this.context = context;
    }

    public static HandshakeProcessResult fromState(SSLEngineResult.HandshakeStatus operation, ChannelContext context) {
        return new HandshakeProcessResult(operation, null, null, false, null, context);
    }

    public static HandshakeProcessResult fromSuccess(
            SSLEngineResult.HandshakeStatus operation,
            SSLEngineResult.HandshakeStatus hsStatus,
            SSLEngineResult.Status processStatus,
            ChannelContext context
    ) {
        return new HandshakeProcessResult(operation, hsStatus, processStatus, false, null, context);
    }

    public static HandshakeProcessResult fromError(SSLEngineResult.HandshakeStatus operation, String err, ChannelContext context) {
        return new HandshakeProcessResult(operation, null,null,true, err, context);
    }
}
