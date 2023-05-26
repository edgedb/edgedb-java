package com.edgedb.driver.binary;

import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.util.BinaryProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.joou.UByte;
import org.joou.UInteger;
import org.joou.ULong;
import org.joou.UShort;

import javax.naming.OperationNotSupportedException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.BiFunction;

import static com.edgedb.driver.util.BinaryProtocolUtils.*;

public class PacketWriter implements AutoCloseable {
    private ByteBuf buffer;
    private final boolean isDynamic;
    private boolean canWrite;

    private interface PrimitiveWriter {
        void write(PacketWriter writer, Number value) throws OperationNotSupportedException;
    }
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final HashMap<Class<?>, PrimitiveWriter> primitiveNumberWriters;

    public PacketWriter(int size, boolean isDynamic) {
        this.isDynamic = isDynamic;
        this.canWrite = true;
        this.buffer = ByteBufAllocator.DEFAULT.directBuffer(size);
    }

    static {
        primitiveNumberWriters = new HashMap<>() {{
            put(Byte.TYPE, (p, v) -> p.write((byte)v));
            put(Byte.class, (p, v) -> p.write((Byte)v));
            put(Short.TYPE, (p, v) -> p.write((short)v));
            put(Short.class, (p, v) -> p.write((Short)v));
            put(Integer.TYPE, (p, v) -> p.write((int)v));
            put(Integer.class, (p, v) -> p.write((Integer)v));
            put(Long.TYPE, (p, v) -> p.write((long)v));
            put(Long.class, (p, v) -> p.write((Long)v));
            put(UByte.class, (p, v) -> p.write((byte)v));
            put(UShort.class, (p, v) -> p.write((short)v));
            put(UInteger.class, (p, v) -> p.write((int)v));
            put(ULong.class, (p, v) -> p.write((long)v));
        }};
    }

    public PacketWriter() {
        this(512, true);
    }

    public PacketWriter(int size) {
        this(size, false);
    }

    public int getPosition() {
        return this.buffer.writerIndex();
    }

    public synchronized void advance(int count) {
        this.buffer.writerIndex(this.buffer.writerIndex() + count);
    }

    public synchronized void seek(int position) {
        this.buffer.writerIndex(position);
    }

    private synchronized void resize(int target) throws OperationNotSupportedException {
        ensureCanWrite();

        if(!isDynamic) {
            throw new IndexOutOfBoundsException(String.format("Cannot write %d bytes as it would overflow the buffer", target));
        }

        var newSize = target + buffer.writerIndex() > 2048
                ? buffer.capacity() + target + 512
                : buffer.capacity() > 2048
                    ? buffer.capacity() + 2048
                    : (buffer.capacity() << 1) + target;

        var newBuffer = ByteBufAllocator.DEFAULT.directBuffer(newSize);

        var position = this.buffer.writerIndex();

        newBuffer.writeBytes(this.buffer);

        this.buffer = newBuffer;

        this.buffer.writerIndex(position);
    }

    private <T> void write(T value, int size, BiFunction<ByteBuf, T, ByteBuf> writer) throws OperationNotSupportedException {
        ensureCanWrite(size);
        // we have to use a bi-func here, since 'ensureCanWrite' can rescale the buffer, changing the
        // 'this.buffer' reference which the delegate can hold an old reference.
        writer.apply(this.buffer, value);
    }

    public void write(double value) throws OperationNotSupportedException {
        write(value, DOUBLE_SIZE, ByteBuf::writeDouble);
    }

    public void write(float value) throws OperationNotSupportedException {
        write(value, FLOAT_SIZE, ByteBuf::writeFloat);
    }

    public void write(long value) throws OperationNotSupportedException {
        write(value, LONG_SIZE, ByteBuf::writeLong);
    }

    public void write(int value) throws OperationNotSupportedException {
        write(value, INT_SIZE, ByteBuf::writeInt);
    }

    public void write(short value) throws OperationNotSupportedException {
        write((int)value, SHORT_SIZE, ByteBuf::writeShort);
    }

    public void write(byte value) throws OperationNotSupportedException {
        write((int)value, BYTE_SIZE, ByteBuf::writeByte);
    }

    public void write(ULong value) throws OperationNotSupportedException {
        this.write(value.longValue());
    }

    public void write(UInteger value) throws OperationNotSupportedException {
        write(value.intValue());
    }

    public void write(UShort value) throws OperationNotSupportedException {
        write(value.shortValue());
    }

    public void write(UByte value) throws OperationNotSupportedException {
        write(value.byteValue());
    }

    public void write(char value) throws OperationNotSupportedException {
        write((int)value, CHAR_SIZE, ByteBuf::writeChar);
    }

    public void write(boolean value) throws OperationNotSupportedException {
        write(value, BOOL_SIZE, ByteBuf::writeBoolean);
    }

    public void write(UUID uuid) throws OperationNotSupportedException {
        write(uuid.getMostSignificantBits());
        write(uuid.getLeastSignificantBits());
    }

    public void write(String value) throws OperationNotSupportedException {
        writeArray(value.getBytes(StandardCharsets.UTF_8));
    }

    public <T extends Number> void writePrimitive(T value) throws OperationNotSupportedException {
        primitiveNumberWriters.get(value.getClass()).write(this, value);
    }

    public void writeArray(ByteBuf buffer) throws OperationNotSupportedException {
        // TODO: ensure that this is the correct way to write a buff to this one

        ensureCanWrite(buffer.writerIndex() + INT_SIZE); // arr length (i32)

        write(buffer.writerIndex());

        if(buffer.writerIndex() > 0) {
            this.buffer.writeBytes(buffer, 0, buffer.writerIndex());
        }
    }

    public void writeArray(byte[] array) throws OperationNotSupportedException {
        if(array.length == 0) {
            write(0);
            return;
        }

        ensureCanWrite(array.length + INT_SIZE); // arr length (i32)
        write(array.length);
        writeArrayWithoutLength(array);
    }

    public void writeArrayWithoutLength(byte[] array) throws OperationNotSupportedException {
        if(array.length == 0) {
            return;
        }

        ensureCanWrite(array.length);
        this.buffer.writeBytes(array);
    }

    public <T extends SerializableData> void write(T serializable) throws OperationNotSupportedException {
        ensureCanWrite(serializable.getSize());
        serializable.write(this);
    }

    public <T extends SerializableData, U extends Number> void writeArray(T[] serializableArray, Class<U> lengthPrimitive) throws OperationNotSupportedException {
        ensureCanWrite(BinaryProtocolUtils.sizeOf(serializableArray, lengthPrimitive));

        var len = BinaryProtocolUtils.castNumber(serializableArray.length, lengthPrimitive);

        primitiveNumberWriters.get(lengthPrimitive).write(this, len);

        for (T serializable : serializableArray) {
            write(serializable);
        }
    }

    public <U extends Number, T extends Enum<T> & BinaryEnum<U>> void writeEnumSet(EnumSet<T> enumSet, Class<U> primitive) throws OperationNotSupportedException {
        long flags = 0L;

        for (T v: enumSet) {
            flags |= v.getValue().longValue();
        }

        primitiveNumberWriters.get(primitive).write(this, BinaryProtocolUtils.castNumber(flags, primitive));
    }

    @FunctionalInterface
    public interface WriterDelegate {
        void consume(PacketWriter writer) throws OperationNotSupportedException, EdgeDBException;
    }

    public void writeDelegateWithLength(WriterDelegate delegate) throws OperationNotSupportedException, EdgeDBException {
        ensureCanWrite(INT_SIZE);

        var currentIndex = this.getPosition();
        this.advance(INT_SIZE);
        delegate.consume(this);
        var lastIndex = this.getPosition();
        seek(currentIndex);
        write((int)(lastIndex - currentIndex - INT_SIZE));
        seek(lastIndex);
    }

    private void ensureCanWrite(int size) throws OperationNotSupportedException {
        ensureCanWrite();

        if((this.buffer.capacity() - this.buffer.writerIndex()) < size) {
            resize(size);
        }
    }
    private void ensureCanWrite() throws OperationNotSupportedException {
        if(!canWrite) {
            throw new OperationNotSupportedException("Cannot use a closed packet writer");
        }
    }

    public ByteBuf getBuffer() {
        close();
        return this.buffer;
    }

    @Override
    public void close() {
        this.canWrite = false;
    }
}
