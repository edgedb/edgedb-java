package com.edgedb.driver.binary.descriptors;

import com.edgedb.driver.binary.PacketReader;

import java.util.UUID;

public final class BaseScalarTypeDescriptor implements TypeDescriptor {
    private final UUID id;

    public BaseScalarTypeDescriptor(final UUID id, final PacketReader ignored) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
