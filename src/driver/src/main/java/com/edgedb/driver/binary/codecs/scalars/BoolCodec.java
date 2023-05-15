package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.PacketReader;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public final class BoolCodec extends ScalarCodecBase<Boolean> {
    public BoolCodec() {
        super(Boolean.class);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Boolean value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public Boolean deserialize(PacketReader reader, CodecContext context) {
        return reader.readBoolean();
    }
}
