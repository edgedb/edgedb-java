package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.KeyValue;
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
