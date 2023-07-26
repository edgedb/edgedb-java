package com.edgedb.driver.binary.protocol.v1.receivables;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.binary.protocol.Receivable;
import com.edgedb.driver.binary.protocol.common.Annotation;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.protocol.ServerMessageType;
import com.edgedb.driver.binary.protocol.common.Cardinality;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

public class CommandDataDescription implements Receivable {
    public final Annotation @NotNull [] annotations;
    public final @NotNull EnumSet<Capabilities> capabilities;
    public final Cardinality cardinality;
    public final @NotNull UUID inputTypeDescriptorId;
    public final @NotNull UUID outputTypeDescriptorId;
    public final @Nullable ByteBuf inputTypeDescriptorBuffer;
    public final @Nullable ByteBuf outputTypeDescriptorBuffer;

    public CommandDataDescription(@NotNull PacketReader reader) {
        annotations = reader.readAnnotations();
        capabilities = reader.readEnumSet(Capabilities.class, Long.TYPE);
        cardinality = reader.readEnum(Cardinality.class, Byte.TYPE);
        inputTypeDescriptorId = reader.readUUID();
        inputTypeDescriptorBuffer = reader.readByteArray();
        outputTypeDescriptorId = reader.readUUID();
        outputTypeDescriptorBuffer = reader.readByteArray();
    }

    @Override
    public void close() {
        if(inputTypeDescriptorBuffer != null) {
            inputTypeDescriptorBuffer.release();
        }

        if(outputTypeDescriptorBuffer != null) {
            outputTypeDescriptorBuffer.release();
        }
    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.COMMAND_DATA_DESCRIPTION;
    }
}
