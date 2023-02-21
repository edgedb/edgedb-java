package com.edgedb.driver.binary;

import com.edgedb.driver.binary.packets.shared.Annotation;
import com.edgedb.driver.binary.packets.shared.KeyValue;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PacketReader {
    private final ByteBuffer buffer;
    private static final Map<Class<?>, Function<PacketReader, ?>> numberReaderMap;

    public PacketReader(ByteBuffer buffer) {
        buffer.flip();
        this.buffer = buffer;
    }

    static {
        numberReaderMap = new HashMap<>();
        numberReaderMap.put(Byte.TYPE, PacketReader::readByte);
        numberReaderMap.put(Short.TYPE, PacketReader::readInt16);
        numberReaderMap.put(Integer.TYPE, PacketReader::readInt32);
        numberReaderMap.put(Long.TYPE, PacketReader::readInt64);
        numberReaderMap.put(Float.TYPE, PacketReader::readFloat);
        numberReaderMap.put(Double.TYPE, PacketReader::readDouble);
    }

    public void skip(int count) {
        this.buffer.position(this.buffer.position() + count);
    }

    public boolean isEmpty() {
        return !this.buffer.hasRemaining();
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

    public Byte readByte() {
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

    public String[] readStringArray() {
        var count = readInt32();

        var arr = new String[count];

        for (int i = 0; i < count; i++) {
            arr[i] = readString();
        }

        return arr;
    }

    public ByteBuffer readByteArray() {
        var len = readInt32();
       return readBytes(len);
    }

    public ByteBuffer readBytes(int length) {
        var buff = this.buffer.slice();
        buff.compact();
        buff.limit(length);
        return buff;
    }

    public Annotation[] readAnnotations() {
        return readArrayOf(Annotation.class, Annotation::new, Short.TYPE);
    }

    public KeyValue[] readAttributes() {
        return readArrayOf(KeyValue.class, KeyValue::new, Short.TYPE);
    }

    @SuppressWarnings("unchecked")
    public <U extends Number, T> T[] readArrayOf(Class<T> cls, Function<PacketReader, T> mapper, Class<U> lengthPrimitive) {
        int len = lengthPrimitive.cast(numberReaderMap.get(lengthPrimitive).apply(this)).intValue();

        // can only use 32 bit, so cast to that
        var arr = (T[]) Array.newInstance(cls, len);

        for(int i = 0; i < len; i++) {
            arr[i] = mapper.apply(this);
        }

        return arr;
    }

    @SuppressWarnings("unchecked")
    public <U extends Number, T extends Enum<T> & BinaryEnum<U>> T readEnum(Function<U, T> mapper, Class<U> primitive) {
        var value = (U)numberReaderMap.get(primitive).apply(this);
        return mapper.apply(value);
    }

    public <U extends Number, T extends Enum<T> & BinaryEnum<U>> EnumSet<T> readEnumSet(Class<T> cls, Class<U> primitive, Function<U, T> map) {
        var value = primitive.cast(numberReaderMap.get(primitive).apply(this));

        var flagBits = Arrays.stream(cls.getEnumConstants())
                .map(BinaryEnum::getValue)
                .filter((u) -> {
                    // assume we can use 64 bit here, should not be any IEEE float/double numbers ever :)
                    return (u.longValue() & value.longValue()) == u.longValue();
                })
                .map(map);

        return EnumSet.copyOf(flagBits.collect(Collectors.toSet()));
    }
}
