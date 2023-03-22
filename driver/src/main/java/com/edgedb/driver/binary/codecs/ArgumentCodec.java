package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.Map;

public interface ArgumentCodec<T> extends Codec<T> {
    void serializeArguments(final PacketWriter writer, @Nullable Map<String, ?> value, CodecContext context) throws EdgeDBException, OperationNotSupportedException;
}
