package com.gel.driver.binary.protocol.v1.receivables;

import com.gel.driver.binary.protocol.Receivable;
import com.gel.driver.binary.protocol.common.Annotation;
import com.gel.driver.ErrorCode;
import com.gel.driver.LogSeverity;
import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.ServerMessageType;
import org.jetbrains.annotations.NotNull;

public class LogMessage implements Receivable {
    public final LogSeverity severity;
    public final ErrorCode code;
    public final @NotNull String content;
    public final Annotation @NotNull [] annotations;


    public LogMessage(@NotNull PacketReader reader) {
        severity = reader.readEnum(LogSeverity.class, Byte.TYPE);
        code = reader.readEnum(ErrorCode.class, Integer.TYPE);
        content = reader.readString();
        annotations = reader.readAnnotations();
    }

    public String format() {
        return String.format("%s: %s", severity, content);
    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.LOG_MESSAGE;
    }
}
