package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.PacketReader;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public final class Float64Codec extends ScalarCodecBase<Double> {
    public Float64Codec() {
        super(Double.class);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Double value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public Double deserialize(PacketReader reader, CodecContext context) {
        return reader.readDouble();
    }
}
