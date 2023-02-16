package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.ErrorCode;
import com.edgedb.driver.ErrorSeverity;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.KeyValue;

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
