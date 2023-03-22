package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Type;

public interface Codec<T> {
    void serialize(final PacketWriter writer, final @Nullable T value, final CodecContext context) throws OperationNotSupportedException, EdgeDBException;
    @Nullable T deserialize(final PacketReader reader, final CodecContext context) throws EdgeDBException, OperationNotSupportedException;

    Class<T> getConvertingClass();
    boolean canConvert(Type type);
}
