package com.edgedb.binary.codecs.scalars;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.PacketReader;
import com.edgedb.binary.codecs.CodecContext;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public final class Integer64Codec extends ScalarCodecBase<Long> {
    public Integer64Codec() {
        super(Long.TYPE);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Long value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public Long deserialize(PacketReader reader, CodecContext context) {
        return reader.readInt64();
    }
}
