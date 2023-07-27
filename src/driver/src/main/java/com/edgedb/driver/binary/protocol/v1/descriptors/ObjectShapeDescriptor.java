package com.edgedb.driver.binary.protocol.v1.descriptors;

import com.edgedb.driver.binary.protocol.TypeDescriptor;
import com.edgedb.driver.binary.protocol.v1.descriptors.common.ShapeElement;
import com.edgedb.driver.binary.PacketReader;
import org.jetbrains.annotations.NotNull;
import org.joou.UShort;

import java.util.UUID;

public final class ObjectShapeDescriptor implements TypeDescriptor {
    public final ShapeElement @NotNull [] shapes;

    private final UUID id;

    public ObjectShapeDescriptor(final UUID id, final @NotNull PacketReader reader) {
        this.id = id;
        this.shapes = reader.readArrayOf(ShapeElement.class, ShapeElement::new, UShort.class);
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
