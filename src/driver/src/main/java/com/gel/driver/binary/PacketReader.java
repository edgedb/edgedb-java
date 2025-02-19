package com.gel.driver.binary;

import com.gel.driver.binary.protocol.common.Annotation;
import com.gel.driver.binary.protocol.common.KeyValue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joou.UByte;
import org.joou.UInteger;
import org.joou.ULong;
import org.joou.UShort;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.joou.Unsigned.*;

public class PacketReader {
    public static final class ScopedReader extends PacketReader implements AutoCloseable {
        public final boolean isNoData;

        public ScopedReader(@Nullable ByteBuf buffer) {
            super(buffer == null ? Unpooled.EMPTY_BUFFER : buffer);
            isNoData = buffer == null;
        }

        @Override
        public void close() {
            buffer.release();
        }
    }

    protected final @NotNull ByteBuf buffer;
    private static final @NotNull Map<Class<?>, Function<PacketReader, ? extends Number>> numberReaderMap;

    private final int initPos;

    public PacketReader(@NotNull ByteBuf buffer) {
        this.buffer = buffer;
        this.initPos = buffer.readerIndex();
    }

    static {
        numberReaderMap = new HashMap<>();
        numberReaderMap.put(Byte.TYPE, PacketReader::readByte);
        numberReaderMap.put(Short.TYPE, PacketReader::readInt16);
        numberReaderMap.put(Integer.TYPE, PacketReader::readInt32);
        numberReaderMap.put(Long.TYPE, PacketReader::readInt64);
        numberReaderMap.put(UByte.class, PacketReader::readUByte);
        numberReaderMap.put(UShort.class, PacketReader::readUInt16);
        numberReaderMap.put(UInteger.class, PacketReader::readUInt32);
        numberReaderMap.put(ULong.class, PacketReader::readUInt64);
        numberReaderMap.put(Float.TYPE, PacketReader::readFloat);
        numberReaderMap.put(Double.TYPE, PacketReader::readDouble);
    }

    public int position() {
        return buffer.readerIndex() - this.initPos;
    }

    public int size() {
        return position() + this.buffer.readableBytes();
    }

    public void skip(int count) {
        this.buffer.skipBytes(count);
    }

    public void skip(long count) {
        if(count >> 32 == 0) {
            // can convert to int
            skip((int)count);
            return;
        }

        var temp = count;
        do {
            this.buffer.skipBytes((int) temp);
            temp -= Integer.MAX_VALUE;
        }
        while (temp >= Integer.MAX_VALUE);
    }

    public boolean isEmpty() {
        return this.buffer.readableBytes() == 0;
    }

    public byte[] consumeByteArray() {
        var arr = new byte[this.buffer.readableBytes()];
        this.buffer.readBytes(arr);
        return arr;
    }

    public @NotNull UUID readUUID() {
        return new UUID(buffer.readLong(), buffer.readLong());
    }

    public @NotNull String readString() {
        var len = readInt32();
        var buffer = new byte[len];
        this.buffer.readBytes(buffer);
        return new String(buffer, StandardCharsets.UTF_8);
    }

    public boolean readBoolean() {
        return buffer.readBoolean();
    }

    public @NotNull Byte readByte() {
        return buffer.readByte();
    }

    public UByte readUByte() {
        return ubyte(buffer.readByte());
    }

    public char readChar() {
        return buffer.readChar();
    }

    public double readDouble() {
        return buffer.readDouble();
    }

    public float readFloat() {
        return buffer.readFloat();
    }

    public long readInt64() {
        return buffer.readLong();
    }

    public @NotNull ULong readUInt64() {
        return ulong(buffer.readLong());
    }

    public int readInt32() {
        return buffer.readInt();
    }

    public UInteger readUInt32() {
        return uint(buffer.readUnsignedInt());
    }

    public short readInt16() {
        return buffer.readShort();
    }

    public @NotNull UShort readUInt16() {
        return ushort(buffer.readUnsignedShort());
    }

    public String @NotNull [] readStringArray() {
        var count = readInt32();

        var arr = new String[count];

        for (int i = 0; i < count; i++) {
            arr[i] = readString();
        }

        return arr;
    }

    /**
     * Reads the {@code length} number of bytes and creates a new {@linkplain ScopedReader} wrapping the bytes.
     * @param length The number of bytes to read.
     * @return A scoped reader whose close method releases the read bytes.
     */
    public ScopedReader scopedSlice(int length) {
        return new ScopedReader(readBytes(length));
    }

    /**
     * Calls {@code readByteArray()} and creates a new {@linkplain ScopedReader} wrapping the bytes.
     * @return A scoped reader whose close method releases the read bytes.
     */
    public ScopedReader scopedSlice() {
        return new ScopedReader(readByteArray());
    }

    public @Nullable ByteBuf readByteArray() {
        var len = readInt32();

        if(len <= 0) {
            return null;
        }

        return readBytes(len);
    }

    public ByteBuf readBytes(int length) {
        return this.buffer.readRetainedSlice(length);
    }

    public Annotation @NotNull [] readAnnotations() {
        return readArrayOf(Annotation.class, Annotation::new, UShort.class);
    }

    public KeyValue @NotNull [] readAttributes() {
        return readArrayOf(KeyValue.class, KeyValue::new, UShort.class);
    }

    @SuppressWarnings("unchecked")
    public <U extends Number, T> T @NotNull [] readArrayOf(Class<T> cls, @NotNull Function<PacketReader, T> mapper, Class<U> lengthPrimitive) {
        var len = numberReaderMap.get(lengthPrimitive).apply(this).intValue();

        // can only use 32 bit, so cast to that
        var arr = (T[]) Array.newInstance(cls, len);

        for(int i = 0; i < len; i++) {
            arr[i] = mapper.apply(this);
        }

        return arr;
    }

    @SuppressWarnings("unchecked")
    public <U extends Number, T extends Enum<T> & BinaryEnum<U>> T readEnum(@NotNull Class<T> cls, Class<U> primitive) {
        return PacketSerializer.getEnumValue(cls, (U)numberReaderMap.get(primitive).apply(this));
    }

    public <U extends Number, T extends Enum<T> & BinaryEnum<U>> @NotNull EnumSet<T> readEnumSet(@NotNull Class<T> cls, Class<U> primitive) {
        var value = numberReaderMap.get(primitive).apply(this).longValue();

        var set = EnumSet.noneOf(cls);

        for (var enumConstant : cls.getEnumConstants()) {
            if((value & enumConstant.getValue().longValue()) > 0) {
                set.add(enumConstant);
            }
        }

        return set;
    }
}
