package com.edgedb.binary.packets.receivable;

import com.edgedb.binary.PacketReader;
import com.edgedb.binary.packets.ServerMessageType;
import com.edgedb.binary.packets.shared.DumpObjectDescriptor;
import com.edgedb.binary.packets.shared.DumpTypeInfo;
import com.edgedb.binary.packets.shared.KeyValue;
import org.joou.UInteger;
import org.joou.UShort;

public class DumpHeader implements Receivable {
    public final KeyValue[] attributes;
    public final UShort majorVersion;
    public final UShort minorVersion;
    public final String schemaDDL;
    public final DumpTypeInfo[] typeInfo;
    public final DumpObjectDescriptor[] descriptors;

    public DumpHeader(PacketReader reader) {
        attributes = reader.readAttributes();
        majorVersion = reader.readUInt16();
        minorVersion = reader.readUInt16();
        schemaDDL = reader.readString();
        typeInfo = reader.readArrayOf(DumpTypeInfo.class, DumpTypeInfo::new, UInteger.class);
        descriptors = reader.readArrayOf(DumpObjectDescriptor.class, DumpObjectDescriptor::new, UInteger.class);
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.DUMP_HEADER;
    }
}
