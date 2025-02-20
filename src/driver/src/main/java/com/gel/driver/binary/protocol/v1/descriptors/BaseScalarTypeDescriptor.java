package com.gel.driver.binary.protocol.v1.descriptors;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.TypeDescriptor;

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
