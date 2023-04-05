package com.edgedb.binary.descriptors;

import com.edgedb.binary.descriptors.common.ShapeElement;
import com.edgedb.binary.PacketReader;
import org.joou.UShort;

import java.util.UUID;

public final class ObjectShapeDescriptor implements TypeDescriptor {
    public final ShapeElement[] shapes;

    private final UUID id;

    public ObjectShapeDescriptor(final UUID id, final PacketReader reader) {
        this.id = id;
        this.shapes = reader.readArrayOf(ShapeElement.class, ShapeElement::new, UShort.class);
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
