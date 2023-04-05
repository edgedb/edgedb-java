package com.edgedb.binary.descriptors;

import com.edgedb.binary.PacketReader;
import org.joou.UShort;

import java.util.UUID;

public final class SetTypeDescriptor implements TypeDescriptor {
    public final UShort typePosition;

    private final UUID id;

    public SetTypeDescriptor(final UUID id, final PacketReader reader) {
        this.id = id;
        this.typePosition = reader.readUInt16();
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
