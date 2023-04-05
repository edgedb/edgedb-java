package com.edgedb.binary.codecs.scalars;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.PacketReader;
import com.edgedb.binary.codecs.CodecContext;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public final class Integer32Codec extends ScalarCodecBase<Integer> {
    public Integer32Codec() {
        super(Integer.TYPE);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Integer value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public Integer deserialize(PacketReader reader, CodecContext context) {
        return reader.readInt32();
    }
}
