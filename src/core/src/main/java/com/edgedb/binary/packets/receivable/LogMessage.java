package com.edgedb.binary.packets.receivable;

import com.edgedb.binary.packets.shared.Annotation;
import com.edgedb.ErrorCode;
import com.edgedb.LogSeverity;
import com.edgedb.binary.PacketReader;
import com.edgedb.binary.packets.ServerMessageType;

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
