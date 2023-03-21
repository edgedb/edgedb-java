package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public final class BytesCodec extends ScalarCodecBase<byte[]> {
    public BytesCodec() {
        super(byte[].class);
    }

    @Override
    public void serialize(PacketWriter writer, byte @Nullable [] value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.writeArrayWithoutLength(value);
        }
    }

    @Override
    public byte @Nullable [] deserialize(PacketReader reader, CodecContext context) {
        return reader.consumeByteArray();
    }
}
