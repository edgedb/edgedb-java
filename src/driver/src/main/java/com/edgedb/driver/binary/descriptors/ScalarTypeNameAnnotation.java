package com.edgedb.driver.binary.descriptors;

import com.edgedb.driver.binary.PacketReader;

import java.util.UUID;

public final class ScalarTypeNameAnnotation implements TypeDescriptor {
    public final String name;

    private final UUID id;

    public ScalarTypeNameAnnotation(final UUID id, final PacketReader reader) {
        this.id = id;
        this.name = reader.readString();
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
