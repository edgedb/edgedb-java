package com.edgedb.driver.binary.packets.shared;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.SerializableData;

import javax.naming.OperationNotSupportedException;
import java.nio.ByteBuffer;

import static com.edgedb.driver.util.BinaryProtocolUtils.SHORT_SIZE;
import static com.edgedb.driver.util.BinaryProtocolUtils.sizeOf;

public class KeyValue implements SerializableData {
    public final short code;
    public final ByteBuffer value;

    public KeyValue(short code, ByteBuffer value) {
        this.code = code;
        this.value = value;
    }

    public KeyValue(PacketReader reader) {
        this.code = reader.readInt16();
        this.value = reader.readByteArray();
    }

    @Override
    public void write(PacketWriter writer) throws OperationNotSupportedException {
        writer.write(code);
        writer.writeArray(value);
    }

    @Override
    public int getSize() {
        return SHORT_SIZE + sizeOf(value);
    }
}
