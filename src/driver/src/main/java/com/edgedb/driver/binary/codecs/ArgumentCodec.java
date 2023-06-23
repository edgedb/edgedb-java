package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.exceptions.EdgeDBException;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.Map;

public interface ArgumentCodec<T> extends Codec<T> {
    void serializeArguments(final PacketWriter writer, @Nullable Map<String, ?> value, CodecContext context) throws EdgeDBException, OperationNotSupportedException;
    static <T> ByteBuf serializeToBuffer(@NotNull ArgumentCodec<T> codec, final @Nullable Map<String, ?> value, final CodecContext context) throws OperationNotSupportedException, EdgeDBException {
        try (var writer = new PacketWriter()) {
            codec.serializeArguments(writer, value, context);
            return writer.getBuffer();
        }
    }
}
