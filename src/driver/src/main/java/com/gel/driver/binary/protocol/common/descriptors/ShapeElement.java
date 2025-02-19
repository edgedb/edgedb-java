package com.gel.driver.binary.protocol.common.descriptors;

import com.gel.driver.binary.protocol.common.Cardinality;
import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.common.descriptors.ShapeElementFlags;
import org.jetbrains.annotations.NotNull;
import org.joou.UInteger;
import org.joou.UShort;

import java.util.EnumSet;

public final class ShapeElement {
    public final @NotNull EnumSet<ShapeElementFlags> flags;
    public final Cardinality cardinality;
    public final @NotNull String name;
    public final @NotNull UShort typePosition;

    public ShapeElement(final @NotNull PacketReader reader) {
        this.flags = reader.readEnumSet(ShapeElementFlags.class, UInteger.class);
        this.cardinality = reader.readEnum(Cardinality.class, Byte.TYPE);
        this.name = reader.readString();
        this.typePosition = reader.readUInt16();
    }
}
