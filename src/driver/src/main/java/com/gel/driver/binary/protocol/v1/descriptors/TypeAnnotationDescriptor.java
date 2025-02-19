package com.gel.driver.binary.protocol.v1.descriptors;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.TypeDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class TypeAnnotationDescriptor implements TypeDescriptor {
    public final DescriptorType type;
    public final @NotNull String annotation;

    private final UUID id;

    public TypeAnnotationDescriptor(final DescriptorType type, final UUID id, final @NotNull PacketReader reader) {
        this.id = id;
        this.type = type;
        this.annotation = reader.readString();
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
