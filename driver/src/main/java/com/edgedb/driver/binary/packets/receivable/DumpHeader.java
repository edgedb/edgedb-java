package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.DumpObjectDescriptor;
import com.edgedb.driver.binary.packets.shared.DumpTypeInfo;
import com.edgedb.driver.binary.packets.shared.KeyValue;

public class DumpHeader implements Receivable {
    public final KeyValue[] attributes;
    public final short majorVersion;
    public final short minorVersion;
    public final String schemaDDL;
    public final DumpTypeInfo[] typeInfo;
    public final DumpObjectDescriptor[] descriptors;

    public DumpHeader(PacketReader reader) {
        attributes = reader.readAttributes();
        majorVersion = reader.readInt16();
        minorVersion = reader.readInt16();
        schemaDDL = reader.readString();
        typeInfo = reader.readArrayOf(DumpTypeInfo.class, DumpTypeInfo::new, Integer.TYPE);
        descriptors = reader.readArrayOf(DumpObjectDescriptor.class, DumpObjectDescriptor::new, Integer.TYPE);
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.DUMP_HEADER;
    }
}
