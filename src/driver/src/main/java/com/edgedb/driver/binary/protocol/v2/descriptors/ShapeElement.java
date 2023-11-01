package com.edgedb.driver.binary.protocol.v2.descriptors;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.protocol.common.Cardinality;
import com.edgedb.driver.binary.protocol.common.descriptors.ShapeElementFlags;
import org.joou.UInteger;
import org.joou.UShort;

public final class ShapeElement {
    public final ShapeElementFlags flags;
    public final Cardinality cardinality;
    public final String name;
    public final UShort typePosition;
    public final UShort sourceTypePosition;

    public ShapeElement(PacketReader reader) {
        this.flags = reader.readEnum(ShapeElementFlags.class, UInteger.class);
        this.cardinality = reader.readEnum(Cardinality.class, Byte.TYPE);
        this.name = reader.readString();
        this.typePosition = reader.readUInt16();
        this.sourceTypePosition = reader.readUInt16();
    }
}
