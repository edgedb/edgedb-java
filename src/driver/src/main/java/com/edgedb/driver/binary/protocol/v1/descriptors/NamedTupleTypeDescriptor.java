package com.edgedb.driver.binary.protocol.v1.descriptors;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.protocol.v1.descriptors.common.TupleElement;
import org.jetbrains.annotations.NotNull;
import org.joou.UShort;

import java.util.UUID;

public final class NamedTupleTypeDescriptor implements TypeDescriptor {
    public final TupleElement @NotNull [] elements;

    private final UUID id;

    public NamedTupleTypeDescriptor(final UUID id, final @NotNull PacketReader reader) {
        this.id = id;
        this.elements = reader.readArrayOf(TupleElement.class, TupleElement::new, UShort.class);
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
