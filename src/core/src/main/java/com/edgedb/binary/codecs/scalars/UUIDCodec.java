package com.edgedb.binary.codecs.scalars;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.PacketReader;
import com.edgedb.binary.codecs.CodecContext;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.UUID;

public final class UUIDCodec extends ScalarCodecBase<UUID> {
    public UUIDCodec() {
        super(UUID.class);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable UUID value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Nullable
    @Override
    public UUID deserialize(PacketReader reader, CodecContext context) {
        return reader.readUUID();
    }
}
