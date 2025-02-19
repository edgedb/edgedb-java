package com.gel.driver.binary.protocol.v1.receivables;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.Receivable;
import com.gel.driver.binary.protocol.ServerMessageType;
import com.gel.driver.binary.protocol.common.KeyValue;
import org.jetbrains.annotations.NotNull;

public class DumpBlock implements Receivable {
    public final KeyValue @NotNull [] attributes;

    public DumpBlock(@NotNull PacketReader reader) {
        attributes = reader.readAttributes();
    }

    @Override
    public void close() throws Exception {
        release(attributes);
    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.DUMP_BLOCK;
    }
}
