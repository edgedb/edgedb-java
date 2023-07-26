package com.edgedb.driver.binary.protocol.v1.receivables;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.protocol.Receivable;
import com.edgedb.driver.binary.protocol.ServerMessageType;
import com.edgedb.driver.binary.protocol.common.KeyValue;
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
