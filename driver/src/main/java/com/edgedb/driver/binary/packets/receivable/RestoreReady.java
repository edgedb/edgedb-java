package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.Annotation;
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
