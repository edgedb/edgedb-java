package com.edgedb.binary.packets.receivable;

import com.edgedb.binary.packets.shared.Annotation;
import com.edgedb.binary.PacketReader;
import com.edgedb.binary.packets.ServerMessageType;
import org.joou.UShort;

public class RestoreReady implements Receivable {
    public final Annotation[] annotations;
    public final UShort jobs;

    public RestoreReady(PacketReader reader) {
        annotations = reader.readAnnotations();
        jobs = reader.readUInt16();
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.RESTORE_READY;
    }
}
