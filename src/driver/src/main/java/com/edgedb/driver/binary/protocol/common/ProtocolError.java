package com.edgedb.driver.binary.protocol.common;

import com.edgedb.driver.ErrorCode;
import com.edgedb.driver.ErrorSeverity;
import org.joou.UShort;

import java.util.Optional;

public interface ProtocolError {
    ErrorSeverity getSeverity();
    ErrorCode getErrorCode();
    String getMessage();

    Optional<KeyValue> tryGetAttribute(short code);
}
