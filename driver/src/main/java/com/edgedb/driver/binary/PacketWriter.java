package com.edgedb.driver.binary;

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
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.edgedb.driver.util.BinaryProtocolUtils.*;

public class PacketWriter implements AutoCloseable {
    private ByteBuf buffer;
    private final boolean isDynamic;
    private boolean canWrite;

    private static final Map<Class<?>, BiConsumer<PacketWriter, Number>> primitiveNumberWriters;

    public PacketWriter(int size, boolean isDynamic) {
        this.isDynamic = isDynamic;
        this.canWrite = true;
        this.buffer = ByteBufAllocator.DEFAULT.directBuffer(size);
    }

    static {
        primitiveNumberWriters = new HashMap<>();

        primitiveNumberWriters.put(Byte.TYPE, (p, v) -> {
            try {
                p.write((byte)v);
            } catch (OperationNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });

        primitiveNumberWriters.put(Short.TYPE, (p, v) -> {
            try {
                p.write((short)v);
            } catch (OperationNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });

        primitiveNumberWriters.put(Integer.TYPE, (p, v) -> {
            try {
                p.write((int) v);
            } catch (OperationNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });

        primitiveNumberWriters.put(Long.TYPE, (p, v) -> {
            try {
                p.write((long) v);
            } catch (OperationNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });

        primitiveNumberWriters.put(UByte.class, (p, v) -> {
            try {
                p.write((byte)v);
            } catch (OperationNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });

        primitiveNumberWriters.put(UShort.class, (p, v) -> {
            try {
                p.write((short)v);
            } catch (OperationNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });

        primitiveNumberWriters.put(UInteger.class, (p, v) -> {
            try {
                p.write((int) v);
            } catch (OperationNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });

        primitiveNumberWriters.put(ULong.class, (p, v) -> {
            try {
                p.write((long) v);
            } catch (OperationNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });
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

    private <T> void write(T value, int size, Consumer<T> writer) throws OperationNotSupportedException {
        ensureCanWrite(size);
        writer.accept(value);
    }

    public void write(double value) throws OperationNotSupportedException {
        write(value, DOUBLE_SIZE, this.buffer::writeDouble);
    }

    public void write(float value) throws OperationNotSupportedException {
        write(value, FLOAT_SIZE, this.buffer::writeFloat);
    }

    public void write(long value) throws OperationNotSupportedException {
        write(value, LONG_SIZE, this.buffer::writeLong);
    }

    public void write(int value) throws OperationNotSupportedException {
        write(value, INT_SIZE, this.buffer::writeInt);
    }

    public void write(short value) throws OperationNotSupportedException {
        write((int)value, SHORT_SIZE, this.buffer::writeShort);
    }

    public void write(byte value) throws OperationNotSupportedException {
        write((int)value, BYTE_SIZE, this.buffer::writeByte);
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
        write((int)value, CHAR_SIZE, this.buffer::writeChar);
    }

    public void write(boolean value) throws OperationNotSupportedException {
        write(value, BOOL_SIZE, this.buffer::writeBoolean);
    }

    public void write(UUID uuid) throws OperationNotSupportedException {
        write(uuid.getMostSignificantBits());
        write(uuid.getLeastSignificantBits());
    }

    public void write(String value) throws OperationNotSupportedException {
        writeArray(value.getBytes(StandardCharsets.UTF_8));
    }

    public void writeArray(ByteBuf buffer) throws OperationNotSupportedException {
        // TODO: ensure that this is the correct way to write a buff to this one

        ensureCanWrite(buffer.writerIndex() + INT_SIZE); // arr length (i32)

        write(buffer.writerIndex());
        this.buffer.writeBytes(buffer, 0, buffer.writerIndex());
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

        primitiveNumberWriters.get(lengthPrimitive).accept(this, len);

        for (T serializable : serializableArray) {
            write(serializable);
        }
    }

    public <U extends Number, T extends Enum<T> & BinaryEnum<U>> void writeEnumSet(EnumSet<T> enumSet, Class<U> primitive) throws OperationNotSupportedException {
        long flags = 0L;

        for (T v: enumSet) {
            flags |= v.getValue().longValue();
        }

        // convert back to U
        U actualFlags;
        Object temp;
        if(primitive.equals(Long.class)) {
            temp = flags;
        } else if (primitive.equals(Integer.class)) {
            temp = (int) flags;
        } else if (primitive.equals(Short.class)) {
            temp = (short) flags;
        } else if (primitive.equals(Byte.class)) {
            temp = (byte) flags;
        } else {
            throw new OperationNotSupportedException("Cannot use enum with primitive type " + primitive.getName());
        }

        actualFlags = primitive.cast(temp);
        primitiveNumberWriters.get(primitive).accept(this, actualFlags);
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
