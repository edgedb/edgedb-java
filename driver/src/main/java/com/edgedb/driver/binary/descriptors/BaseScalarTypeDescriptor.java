package com.edgedb.driver.binary.descriptors;

import java.util.UUID;

public class BaseScalarTypeDescriptor implements TypeDescriptor {
    private final UUID id;

    public BaseScalarTypeDescriptor(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
