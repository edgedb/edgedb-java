package com.edgedb.driver.binary;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PacketReader {
    private final ByteBuffer buffer;

    public PacketReader(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public UUID readUUID() {
        return new UUID(buffer.getLong(), buffer.getLong());
    }
    public String readString() {
        var len = readInt32();
        var buffer = new byte[len];
        this.buffer.get(buffer);
        return new String(buffer, StandardCharsets.UTF_8);
    }

    public boolean readBoolean() {
        return buffer.get() > 0;
    }

    public byte readByte() {
        return buffer.get();
    }

    public char readChar() {
        return buffer.getChar();
    }

    public double readDouble() {
        return buffer.getDouble();
    }

    public float readFloat() {
        return buffer.getFloat();
    }

    public long readInt64() {
        return buffer.getLong();
    }

    public int readInt32() {
        return buffer.getInt();
    }

    public short readInt16() {
        return buffer.getShort();
    }
}
