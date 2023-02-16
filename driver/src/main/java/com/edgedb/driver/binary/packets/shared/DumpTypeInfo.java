package com.edgedb.driver.binary.packets.shared;

import com.edgedb.driver.binary.PacketReader;

import java.util.UUID;

public class DumpTypeInfo {
    public final String name;
    public final String className;
    public final UUID id;

    public DumpTypeInfo(PacketReader reader) {
        name = reader.readString();
        className = reader.readString();
        id = reader.readUUID();
    }

}
