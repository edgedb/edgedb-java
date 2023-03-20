package com.edgedb.driver.binary.descriptors;

import com.edgedb.driver.binary.PacketReader;

import java.util.UUID;

public final class EnumerationTypeDescriptor implements TypeDescriptor {
    public final String[] members;

    private final UUID id;

    public EnumerationTypeDescriptor(UUID id, final PacketReader reader) {
        this.id = id;

        this.members = reader.readArrayOf(String.class, PacketReader::readString, Short.TYPE);
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
