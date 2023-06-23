package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.CodecContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.UUID;

public final class UUIDCodec extends ScalarCodecBase<UUID> {
    public UUIDCodec() {
        super(UUID.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable UUID value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public @NotNull UUID deserialize(@NotNull PacketReader reader, CodecContext context) {
        return reader.readUUID();
    }
}
