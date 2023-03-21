package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public final class Integer16Codec extends ScalarCodecBase<Short> {
    public Integer16Codec() {
        super(Short.TYPE);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Short value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public Short deserialize(PacketReader reader, CodecContext context) {
        return reader.readInt16();
    }
}
