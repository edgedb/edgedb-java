package com.edgedb.driver.binary;

import com.edgedb.driver.util.BinaryProtocolUtils;
import org.jetbrains.annotations.NotNull;

import javax.naming.OperationNotSupportedException;

public interface BinaryEnum<T extends Number> extends SerializableData {
    T getValue();

    default void write(final @NotNull PacketWriter writer) throws OperationNotSupportedException {
        writer.writePrimitive(getValue());
    }

    default int getSize() {
        return BinaryProtocolUtils.sizeOf(getValue().getClass());
    }
}
