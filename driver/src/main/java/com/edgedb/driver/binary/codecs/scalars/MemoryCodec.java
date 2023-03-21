package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.datatypes.Memory;
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
