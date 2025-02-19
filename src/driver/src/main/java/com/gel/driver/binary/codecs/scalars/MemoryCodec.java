package com.gel.driver.binary.codecs.scalars;

import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.codecs.CodecContext;
import com.gel.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.gel.driver.datatypes.Memory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.UUID;

public final class MemoryCodec extends ScalarCodecBase<Memory> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000130");

    public MemoryCodec(@Nullable CodecMetadata metadata) {
        super(ID, metadata, Memory.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Memory value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value.getBytes());
        }
    }

    @Override
    public @NotNull Memory deserialize(@NotNull PacketReader reader, CodecContext context) {
        return new Memory(reader.readInt64());
    }
}
