package com.edgedb.binary.descriptors.common;

import com.edgedb.binary.packets.shared.Cardinality;
import com.edgedb.binary.PacketReader;
import org.joou.UInteger;
import org.joou.UShort;

import java.util.EnumSet;

public final class ShapeElement {
    public final EnumSet<ShapeElementFlags> flags;
    public final Cardinality cardinality;
    public final String name;
    public final UShort typePosition;

    public ShapeElement(final PacketReader reader) {
        this.flags = reader.readEnumSet(ShapeElementFlags.class, UInteger.class, ShapeElementFlags::valueOf);
        this.cardinality = reader.readEnum(Cardinality::valueOf, Byte.TYPE);
        this.name = reader.readString();
        this.typePosition = reader.readUInt16();
    }
}
