package com.gel.driver.binary.protocol.v1.receivables;

import com.gel.driver.binary.protocol.Receivable;
import com.gel.driver.binary.protocol.common.Annotation;
import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.ServerMessageType;
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
