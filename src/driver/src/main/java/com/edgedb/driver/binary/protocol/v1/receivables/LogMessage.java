package com.edgedb.driver.binary.protocol.v1.receivables;

import com.edgedb.driver.binary.protocol.Receivable;
import com.edgedb.driver.binary.protocol.common.Annotation;
import com.edgedb.driver.ErrorCode;
import com.edgedb.driver.LogSeverity;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.protocol.ServerMessageType;
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
