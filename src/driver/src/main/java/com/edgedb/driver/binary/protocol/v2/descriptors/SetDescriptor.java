package com.edgedb.driver.binary.protocol.v2.descriptors;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.protocol.TypeDescriptor;
import org.joou.UShort;

import java.util.UUID;

public final class SetDescriptor implements TypeDescriptor {
    public final UUID id;
    public final UShort type;

    public SetDescriptor(UUID id, PacketReader reader) {
        this.id = id;
        this.type = reader.readUInt16();
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
