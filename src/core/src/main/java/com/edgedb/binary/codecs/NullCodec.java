package com.edgedb.binary.codecs;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.PacketReader;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Type;
import java.util.Map;

public final class NullCodec implements Codec<Void>, ArgumentCodec<Void> {
    @Override
    public void serialize(PacketWriter writer, @Nullable Void value, CodecContext context) throws OperationNotSupportedException {
        writer.write(0);
    }

    @Nullable
    @Override
    public Void deserialize(PacketReader reader, CodecContext context) {
        return null;
    }

    @Override
    public Class<Void> getConvertingClass() {
        return Void.class;
    }

    @Override
    public boolean canConvert(Type type) {
        return true;
    }

    @Override
    public void serializeArguments(PacketWriter writer, @Nullable Map<String, ?> value, CodecContext context) {}
}
