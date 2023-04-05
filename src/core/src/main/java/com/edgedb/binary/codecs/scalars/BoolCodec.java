package com.edgedb.binary.codecs.scalars;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.codecs.CodecContext;
import com.edgedb.binary.PacketReader;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public final class BoolCodec extends ScalarCodecBase<Boolean> {
    public BoolCodec() {
        super(Boolean.TYPE);
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
