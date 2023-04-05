package com.edgedb.binary.packets.shared;

import com.edgedb.binary.PacketReader;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class DumpObjectDescriptor {
    public final UUID objectId;
    public final ByteBuf description;
    public final UUID[] dependencies;

    public DumpObjectDescriptor(PacketReader reader) {
        objectId = reader.readUUID();
        description = reader.readByteArray();
        dependencies = reader.readArrayOf(UUID.class, PacketReader::readUUID, Short.class);
    }

}
