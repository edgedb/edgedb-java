package com.gel.driver.binary.protocol.v1.receivables;

import com.gel.driver.ErrorCode;
import com.gel.driver.ErrorSeverity;
import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.Receivable;
import com.gel.driver.binary.protocol.ServerMessageType;
import com.gel.driver.binary.protocol.common.KeyValue;
import com.gel.driver.binary.protocol.common.ProtocolError;
import com.gel.driver.exceptions.GelErrorException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ErrorResponse implements Receivable, ProtocolError {
    public final ErrorSeverity severity;
    public final ErrorCode errorCode;
    public final @NotNull String message;
    public final KeyValue @NotNull [] attributes;

    public final Map<Short, KeyValue> attributesMap;

    public ErrorResponse(@NotNull PacketReader reader) {
        severity = reader.readEnum(ErrorSeverity.class, Byte.TYPE);
        errorCode = reader.readEnum(ErrorCode.class, Integer.TYPE);
        message = reader.readString();
        attributes = reader.readAttributes();

        attributesMap = new HashMap<>() {{
            for (int i = 0; i != attributes.length; i++) {
                put(attributes[i].code, attributes[i]);
            }
        }};

    }

    public @NotNull GelErrorException toException() {
        return toException(null);
    }
    public @NotNull GelErrorException toException(@Nullable String query) {
        return new GelErrorException(
                Arrays.stream(attributes).collect(Collectors.toMap(v -> v.code, v -> {
                    if(v.value == null) {
                        return new byte[0];
                    }

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
    public void close() throws Exception {
        release(attributes);
        attributesMap.clear();

    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.ERROR_RESPONSE;
    }

    @Override
    public ErrorSeverity getSeverity() {
        return this.severity;
    }

    @Override
    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    @Override
    public @NotNull String getMessage() {
        return this.message;
    }

    @Override
    public Optional<KeyValue> tryGetAttribute(short code) {
        if(!attributesMap.containsKey(code)) {
            return Optional.empty();
        }

        return Optional.of(attributesMap.get(code));
    }
}
