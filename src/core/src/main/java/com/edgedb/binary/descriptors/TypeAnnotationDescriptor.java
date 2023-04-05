package com.edgedb.binary.descriptors;

import com.edgedb.binary.PacketReader;

import java.util.UUID;

public final class TypeAnnotationDescriptor implements TypeDescriptor {
    public final DescriptorType type;
    public final String annotation;

    private final UUID id;

    public TypeAnnotationDescriptor(final DescriptorType type, final UUID id, final PacketReader reader) {
        this.id = id;
        this.type = type;
        this.annotation = reader.readString();
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
