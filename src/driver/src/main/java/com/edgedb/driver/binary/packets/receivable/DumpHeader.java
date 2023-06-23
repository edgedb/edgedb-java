package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.DumpObjectDescriptor;
import com.edgedb.driver.binary.packets.shared.DumpTypeInfo;
import com.edgedb.driver.binary.packets.shared.KeyValue;
import org.jetbrains.annotations.NotNull;
import org.joou.UInteger;
import org.joou.UShort;

public class DumpHeader implements Receivable {
    public final KeyValue @NotNull [] attributes;
    public final @NotNull UShort majorVersion;
    public final @NotNull UShort minorVersion;
    public final @NotNull String schemaDDL;
    public final DumpTypeInfo @NotNull [] typeInfo;
    public final DumpObjectDescriptor @NotNull [] descriptors;

    public DumpHeader(@NotNull PacketReader reader) {
        attributes = reader.readAttributes();
        majorVersion = reader.readUInt16();
        minorVersion = reader.readUInt16();
        schemaDDL = reader.readString();
        typeInfo = reader.readArrayOf(DumpTypeInfo.class, DumpTypeInfo::new, UInteger.class);
        descriptors = reader.readArrayOf(DumpObjectDescriptor.class, DumpObjectDescriptor::new, UInteger.class);
    }

    @Override
    public void close() throws Exception {
        release(attributes);
        release(descriptors);
    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.DUMP_HEADER;
    }
}
