package com.edgedb.driver.binary.descriptors.common;

import com.edgedb.driver.binary.PacketReader;

public final class TupleElement {
    public final String name;
    public final short typePosition;

    public TupleElement(final PacketReader reader) {
        this.name = reader.readString();
        this.typePosition = reader.readInt16();
    }
}
