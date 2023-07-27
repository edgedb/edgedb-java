package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.PacketReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.UUID;

public final class BoolCodec extends ScalarCodecBase<Boolean> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000109");
    public BoolCodec() {
        super(ID, Boolean.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Boolean value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public @NotNull Boolean deserialize(@NotNull PacketReader reader, CodecContext context) {
        return reader.readBoolean();
    }
}
