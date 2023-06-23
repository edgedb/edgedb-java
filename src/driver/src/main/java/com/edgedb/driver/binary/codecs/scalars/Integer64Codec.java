package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.CodecContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public final class Integer64Codec extends ScalarCodecBase<Long> {
    public Integer64Codec() {
        super(Long.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Long value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public @NotNull Long deserialize(@NotNull PacketReader reader, CodecContext context) {
        return reader.readInt64();
    }
}
