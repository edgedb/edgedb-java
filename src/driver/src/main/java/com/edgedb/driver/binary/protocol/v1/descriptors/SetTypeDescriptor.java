package com.edgedb.driver.binary.protocol.v1.descriptors;

import com.edgedb.driver.binary.PacketReader;
import org.jetbrains.annotations.NotNull;
import org.joou.UShort;

import java.util.UUID;

public final class SetTypeDescriptor implements TypeDescriptor {
    public final @NotNull UShort typePosition;

    private final UUID id;

    public SetTypeDescriptor(final UUID id, final @NotNull PacketReader reader) {
        this.id = id;
        this.typePosition = reader.readUInt16();
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
