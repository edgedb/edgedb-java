package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.packets.shared.Annotation;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
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
