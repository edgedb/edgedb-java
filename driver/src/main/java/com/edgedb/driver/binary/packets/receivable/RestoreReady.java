package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.Annotation;

public class RestoreReady implements Receivable {
    public final Annotation[] annotations;
    public final short jobs;

    public RestoreReady(PacketReader reader) {
        annotations = reader.readAnnotations();
        jobs = reader.readInt16();
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.RESTORE_READY;
    }
}
