package com.edgedb.driver.binary.descriptors;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.descriptors.common.TupleElement;
import org.joou.UShort;

import java.util.UUID;

public final class NamedTupleTypeDescriptor implements TypeDescriptor {
    public final TupleElement[] elements;

    private final UUID id;

    public NamedTupleTypeDescriptor(final UUID id, final PacketReader reader) {
        this.id = id;
        this.elements = reader.readArrayOf(TupleElement.class, TupleElement::new, UShort.class);
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
