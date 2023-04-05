package com.edgedb.binary.codecs.scalars;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.PacketReader;
import com.edgedb.binary.codecs.CodecContext;
import com.edgedb.datatypes.Memory;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public final class MemoryCodec extends ScalarCodecBase<Memory> {
    public MemoryCodec() {
        super(Memory.class);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Memory value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value.bytes);
        }
    }

    @Override
    public Memory deserialize(PacketReader reader, CodecContext context) {
        return new Memory(reader.readInt64());
    }
}
