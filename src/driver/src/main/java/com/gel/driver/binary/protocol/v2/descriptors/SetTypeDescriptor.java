package com.gel.driver.binary.protocol.v2.descriptors;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.TypeDescriptor;
import org.joou.UShort;

import java.util.UUID;

public final class SetTypeDescriptor implements TypeDescriptor {
    public final UUID id;
    public final UShort type;

    public SetTypeDescriptor(UUID id, PacketReader reader) {
        this.id = id;
        this.type = reader.readUInt16();
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
