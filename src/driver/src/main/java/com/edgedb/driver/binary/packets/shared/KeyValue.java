package com.edgedb.driver.binary.packets.shared;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.SerializableData;
import com.edgedb.driver.util.BinaryProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public class KeyValue implements SerializableData, AutoCloseable {
    public final short code;
    public final @Nullable ByteBuf value;

    public KeyValue(short code, @Nullable ByteBuf value) {
        this.code = code;
        this.value = value;
    }

    public KeyValue(@NotNull PacketReader reader) {
        this.code = reader.readInt16();
        this.value = reader.readByteArray();
    }

    @Override
    public void write(@NotNull PacketWriter writer) throws OperationNotSupportedException {
        writer.write(code);
        writer.writeArray(value);
    }

    @Override
    public int getSize() {
        return BinaryProtocolUtils.SHORT_SIZE + BinaryProtocolUtils.sizeOf(value);
    }

    @Override
    public void close() {
        if(value != null) {
            value.release();
        }
    }
}
