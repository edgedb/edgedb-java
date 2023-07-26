package com.edgedb.driver.binary.protocol.common;

import com.edgedb.driver.binary.PacketReader;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DumpObjectDescriptor implements AutoCloseable {
    public final @NotNull UUID objectId;
    public final @Nullable ByteBuf description;
    public final UUID @NotNull [] dependencies;

    public DumpObjectDescriptor(@NotNull PacketReader reader) {
        objectId = reader.readUUID();
        description = reader.readByteArray();
        dependencies = reader.readArrayOf(UUID.class, PacketReader::readUUID, Short.class);
    }

    @Override
    public void close() {
        if(description != null) {
            description.release();
        }
    }
}
