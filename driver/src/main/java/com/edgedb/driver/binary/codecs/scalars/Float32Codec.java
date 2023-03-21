package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public final class Float32Codec extends ScalarCodecBase<Float> {
    public Float32Codec() {
        super(Float.TYPE);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Float value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public Float deserialize(PacketReader reader, CodecContext context) {
        return reader.readFloat();
    }
}
