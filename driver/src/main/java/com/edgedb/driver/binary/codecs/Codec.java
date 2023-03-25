package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.exceptions.EdgeDBException;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Type;

public interface Codec<T> {
    void serialize(final PacketWriter writer, final @Nullable T value, final CodecContext context) throws OperationNotSupportedException, EdgeDBException;
    @Nullable T deserialize(final PacketReader reader, final CodecContext context) throws EdgeDBException, OperationNotSupportedException;

    Class<T> getConvertingClass();
    boolean canConvert(Type type);

    static <T> ByteBuf serializeToBuffer(Codec<T> codec, final @Nullable T value, final CodecContext context) throws OperationNotSupportedException, EdgeDBException {
        try (var writer = new PacketWriter()) {
            codec.serialize(writer, value, context);
            return writer.getBuffer();
        }
    }

    static <T> T deserializeFromBuffer(Codec<T> codec, final ByteBuf buffer, final CodecContext context) throws EdgeDBException, OperationNotSupportedException {
        var reader = new PacketReader(buffer);
        return codec.deserialize(reader, context);
    }
}
