package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.PacketReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public final class Integer16Codec extends ScalarCodecBase<Short> {
    public Integer16Codec() {
        super(Short.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Short value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public @NotNull Short deserialize(@NotNull PacketReader reader, CodecContext context) {
        return reader.readInt16();
    }
}
