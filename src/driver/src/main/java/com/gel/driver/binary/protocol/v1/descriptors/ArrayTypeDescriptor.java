package com.gel.driver.binary.protocol.v1.descriptors;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.TypeDescriptor;
import org.jetbrains.annotations.NotNull;
import org.joou.UInteger;
import org.joou.UShort;

import java.util.UUID;

public final class ArrayTypeDescriptor implements TypeDescriptor {
    public final @NotNull UShort typePosition;
    public final UInteger @NotNull [] dimensions;

    private final UUID id;

    public ArrayTypeDescriptor(final UUID id, final @NotNull PacketReader reader) {
        this.id = id;

        this.typePosition = reader.readUInt16();
        this.dimensions = reader.readArrayOf(UInteger.class, PacketReader::readUInt32, UShort.class);
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
