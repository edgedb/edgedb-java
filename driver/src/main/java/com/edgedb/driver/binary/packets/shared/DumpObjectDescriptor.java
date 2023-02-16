package com.edgedb.driver.binary.packets.shared;

import com.edgedb.driver.binary.PacketReader;

import java.nio.ByteBuffer;
import java.util.UUID;

public class DumpObjectDescriptor {
    public final UUID objectId;
    public final ByteBuffer description;
    public final UUID[] dependencies;

    public DumpObjectDescriptor(PacketReader reader) {
        objectId = reader.readUUID();
        description = reader.readByteArray();
        dependencies = reader.readArrayOf(UUID.class, PacketReader::readUUID, Short.class);
    }

}
