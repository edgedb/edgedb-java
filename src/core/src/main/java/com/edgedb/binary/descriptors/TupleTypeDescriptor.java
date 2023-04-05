package com.edgedb.binary.descriptors;

import com.edgedb.binary.PacketReader;
import org.joou.UShort;

import java.util.UUID;

public final class TupleTypeDescriptor implements TypeDescriptor {

    public final UShort[] elementTypeDescriptorPositions;

    private final UUID id;

    public TupleTypeDescriptor(final UUID id, final PacketReader reader) {
        this.id = id;
        this.elementTypeDescriptorPositions = reader.readArrayOf(UShort.class, PacketReader::readUInt16, UShort.class);
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
