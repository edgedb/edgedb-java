package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.ErrorCode;
import com.edgedb.driver.LogSeverity;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.Annotation;

public class LogMessage implements Receivable {
    public final LogSeverity severity;
    public final ErrorCode code;
    public final String content;
    public final Annotation[] annotations;


    public LogMessage(PacketReader reader) {
        severity = reader.readEnum(LogSeverity::valueOf, Byte.TYPE);
        code = reader.readEnum(ErrorCode::valueOf, Integer.TYPE);
        content = reader.readString();
        annotations = reader.readAnnotations();
    }

    public String format() {
        return String.format("%s: %s", severity, content);
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.LOG_MESSAGE;
    }
}
