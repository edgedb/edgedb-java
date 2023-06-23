package com.edgedb.driver.binary.descriptors;

import com.edgedb.driver.binary.PacketReader;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class EnumerationTypeDescriptor implements TypeDescriptor {
    public final String @NotNull [] members;

    private final UUID id;

    public EnumerationTypeDescriptor(final UUID id, final @NotNull PacketReader reader) {
        this.id = id;

        this.members = reader.readArrayOf(String.class, PacketReader::readString, Short.TYPE);
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
