package com.edgedb.driver.binary.descriptors;

import com.edgedb.driver.binary.PacketReader;
import org.joou.UInteger;
import org.joou.UShort;

import java.util.UUID;

public final class ArrayTypeDescriptor implements TypeDescriptor {
    public final UShort typePosition;
    public final UInteger[] dimensions;

    private final UUID id;

    public ArrayTypeDescriptor(final UUID id, final PacketReader reader) {
        this.id = id;

        this.typePosition = reader.readUInt16();
        this.dimensions = reader.readArrayOf(UInteger.class, PacketReader::readUInt32, UShort.class);
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
