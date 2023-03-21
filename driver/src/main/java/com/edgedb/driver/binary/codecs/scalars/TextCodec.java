package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public final class TextCodec extends ScalarCodecBase<String> {
    public TextCodec() {
        super(String.class);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable String value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public @Nullable String deserialize(PacketReader reader, CodecContext context) {
        return reader.readString();
    }
}
