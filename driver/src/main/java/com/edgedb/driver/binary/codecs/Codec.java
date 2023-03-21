package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Type;

public interface Codec<T> {
    void serialize(final PacketWriter writer, final @Nullable T value, final CodecContext context) throws OperationNotSupportedException;
    @Nullable T deserialize(final PacketReader reader, final CodecContext context);

    Class<T> getConvertingClass();
    boolean canConvert(Type type);
}
