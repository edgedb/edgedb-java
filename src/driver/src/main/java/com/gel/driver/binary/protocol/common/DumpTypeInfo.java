package com.gel.driver.binary.protocol.common;

import com.gel.driver.binary.PacketReader;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DumpTypeInfo {
    public final @NotNull String name;
    public final @NotNull String className;
    public final @NotNull UUID id;

    public DumpTypeInfo(@NotNull PacketReader reader) {
        name = reader.readString();
        className = reader.readString();
        id = reader.readUUID();
    }

}
