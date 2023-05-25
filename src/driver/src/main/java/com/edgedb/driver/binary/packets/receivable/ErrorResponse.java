package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.ErrorCode;
import com.edgedb.driver.ErrorSeverity;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.KeyValue;
import com.edgedb.driver.exceptions.EdgeDBErrorException;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ErrorResponse implements Receivable {
    public final ErrorSeverity severity;
    public final ErrorCode errorCode;
    public final String message;
    public final KeyValue[] attributes;

    public ErrorResponse(PacketReader reader) {
        severity = reader.readEnum(ErrorSeverity.class, Byte.TYPE);
        errorCode = reader.readEnum(ErrorCode.class, Integer.TYPE);
        message = reader.readString();
        attributes = reader.readAttributes();
    }

    public EdgeDBErrorException toException() {
        return toException(null);
    }
    public EdgeDBErrorException toException(@Nullable String query) {
        return new EdgeDBErrorException(
                Arrays.stream(attributes).collect(Collectors.toMap(v -> v.code, v -> {
                    var arr = new byte[v.value.readableBytes()];
                    v.value.readBytes(arr);
                    return arr;
                })),
                message,
                errorCode,
                query
        );
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.ERROR_RESPONSE;
    }
}
