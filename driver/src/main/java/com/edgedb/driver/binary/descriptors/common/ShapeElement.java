package com.edgedb.driver.binary.descriptors.common;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.shared.Cardinality;
import org.joou.UInteger;
import org.joou.UShort;

public final class ShapeElement {
    public final ShapeElementFlags flags;
    public final Cardinality cardinality;
    public final String name;
    public final UShort typePosition;

    public ShapeElement(final PacketReader reader) {
        this.flags = reader.readEnum(ShapeElementFlags::valueOf, UInteger.class);
        this.cardinality = reader.readEnum(Cardinality::valueOf, Byte.TYPE);
        this.name = reader.readString();
        this.typePosition = reader.readUInt16();
    }
}
