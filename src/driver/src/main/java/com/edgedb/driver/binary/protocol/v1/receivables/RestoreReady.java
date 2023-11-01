package com.edgedb.driver.binary.protocol.v1.receivables;

import com.edgedb.driver.binary.protocol.Receivable;
import com.edgedb.driver.binary.protocol.common.Annotation;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.protocol.ServerMessageType;
import org.jetbrains.annotations.NotNull;
import org.joou.UShort;

public class RestoreReady implements Receivable {
    public final Annotation @NotNull [] annotations;
    public final @NotNull UShort jobs;

    public RestoreReady(@NotNull PacketReader reader) {
        annotations = reader.readAnnotations();
        jobs = reader.readUInt16();
    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.RESTORE_READY;
    }
}
