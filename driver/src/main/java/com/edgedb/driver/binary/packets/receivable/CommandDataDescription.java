package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.Annotation;
import com.edgedb.driver.binary.packets.shared.Cardinality;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.UUID;

public class CommandDataDescription implements Receivable {
    public final Annotation[] annotations;
    public final EnumSet<Capabilities> capabilities;
    public final Cardinality cardinality;
    public final UUID inputTypeDescriptorId;
    public final UUID outputTypeDescriptorId;
    public final ByteBuffer inputTypeDescriptorBuffer;
    public final ByteBuffer outputTypeDescriptorBuffer;

    public CommandDataDescription(PacketReader reader) {
        annotations = reader.readAnnotations();
        capabilities = reader.readEnumSet(Capabilities.class, Long.TYPE, Capabilities::valueOf);
        cardinality = reader.readEnum(Cardinality::valueOf, Byte.TYPE);
        inputTypeDescriptorId = reader.readUUID();
        inputTypeDescriptorBuffer = reader.readByteArray();
        outputTypeDescriptorId = reader.readUUID();
        outputTypeDescriptorBuffer = reader.readByteArray();
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.COMMAND_DATA_DESCRIPTION;
    }
}
