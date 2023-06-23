package com.edgedb.driver.binary.descriptors;

import com.edgedb.driver.binary.PacketReader;
import org.jetbrains.annotations.NotNull;
import org.joou.UShort;

import java.util.UUID;

public final class ScalarTypeDescriptor implements TypeDescriptor {
    public final @NotNull UShort baseTypePosition;

    private final UUID id;

    public ScalarTypeDescriptor(final UUID id, final @NotNull PacketReader reader) {
        this.id = id;
        this.baseTypePosition = reader.readUInt16();
    }


    @Override
    public UUID getId() {
        return this.id;
    }
}
