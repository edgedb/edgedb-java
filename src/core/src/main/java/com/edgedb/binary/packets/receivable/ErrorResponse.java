package com.edgedb.binary.packets.receivable;

import com.edgedb.binary.packets.shared.KeyValue;
import com.edgedb.ErrorCode;
import com.edgedb.ErrorSeverity;
import com.edgedb.binary.PacketReader;
import com.edgedb.binary.packets.ServerMessageType;

public class ErrorResponse implements Receivable {
    public final ErrorSeverity severity;
    public final ErrorCode errorCode;
    public final String message;
    public final KeyValue[] attributes;

    public ErrorResponse(PacketReader reader) {
        severity = reader.readEnum(ErrorSeverity::valueOf, Byte.TYPE);
        errorCode = reader.readEnum(ErrorCode::valueOf, Integer.TYPE);
        message = reader.readString();
        attributes = reader.readAttributes();
    }


    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.ERROR_RESPONSE;
    }
}
