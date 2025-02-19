package com.gel.driver.binary.protocol.common.descriptors;

import com.gel.driver.binary.PacketReader;
import org.jetbrains.annotations.NotNull;

public final class TupleElement {
    public final @NotNull String name;
    public final short typePosition;

    public TupleElement(final @NotNull PacketReader reader) {
        this.name = reader.readString();
        this.typePosition = reader.readInt16();
    }
}
