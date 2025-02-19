package com.gel.driver.binary.protocol.common;

import com.gel.driver.ErrorCode;
import com.gel.driver.ErrorSeverity;
import org.joou.UShort;

import java.util.Optional;

public interface ProtocolError {
    ErrorSeverity getSeverity();
    ErrorCode getErrorCode();
    String getMessage();

    Optional<KeyValue> tryGetAttribute(short code);
}
