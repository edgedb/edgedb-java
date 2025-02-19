package com.gel.driver.binary.protocol.v1.descriptors;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.TypeDescriptor;
import org.jetbrains.annotations.NotNull;
import org.joou.UShort;

import java.util.UUID;

public final class RangeTypeDescriptor implements TypeDescriptor {
    public final @NotNull UShort typePosition;

    private final UUID id;

    public RangeTypeDescriptor(final UUID id, final @NotNull PacketReader reader) {
        this.id = id;
        this.typePosition = reader.readUInt16();
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
