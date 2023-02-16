package com.edgedb.driver.binary;

import com.edgedb.driver.util.BinaryProtocolUtils;

import javax.naming.OperationNotSupportedException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.edgedb.driver.util.BinaryProtocolUtils.*;

public class PacketWriter implements AutoCloseable {
    private ByteBuffer buffer;
    private final boolean isDynamic;
    private boolean canWrite;

    private static final Map<Class<?>, BiConsumer<PacketWriter, Number>> primitiveNumberWriters;

    public PacketWriter(int size, boolean isDynamic) {
        this.isDynamic = isDynamic;
        this.canWrite = true;
        this.buffer = ByteBuffer.allocateDirect(size);
    }

    static {
        primitiveNumberWriters = new HashMap<>();

        primitiveNumberWriters.put(Byte.class, (p, v) -> {
            try {
                p.write((byte)v);
            } catch (OperationNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });

        primitiveNumberWriters.put(Short.class, (p, v) -> {
            try {
                p.write((short)v);
            } catch (OperationNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });

        primitiveNumberWriters.put(Integer.class, (p, v) -> {
            try {
                p.write((int) v);
            } catch (OperationNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });

        primitiveNumberWriters.put(Long.class, (p, v) -> {
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
        return this.buffer.position();
    }

    public synchronized void advance(int count) {
        this.seek(this.buffer.position() + count);
    }

    public synchronized void seek(int position) {
        this.buffer.position(position);
    }

    private synchronized void resize(int target) throws OperationNotSupportedException {
        ensureCanWrite();

        if(!isDynamic) {
            throw new IndexOutOfBoundsException(String.format("Cannot write %d bytes as it would overflow the buffer", target));
        }

        var newSize = target + buffer.position() > 2048
                ? buffer.capacity() + target + 512
                : buffer.capacity() > 2048
                    ? buffer.capacity() + 2048
                    : (buffer.capacity() << 1) + target;

        var newBuffer = ByteBuffer.allocateDirect(newSize);

        var position = this.buffer.position();

        this.buffer.rewind();
        newBuffer.put(this.buffer);
        newBuffer.flip();

        this.buffer = newBuffer;

        this.buffer.position(position);
    }

    private <T> void write(T value, int size, Consumer<T> writer) throws OperationNotSupportedException {
        ensureCanWrite(size);
        writer.accept(value);
    }

    public void write(double value) throws OperationNotSupportedException {
        write(value, DOUBLE_SIZE, this.buffer::putDouble);
    }

    public void write(float value) throws OperationNotSupportedException {
        write(value, FLOAT_SIZE, this.buffer::putFloat);
    }

    public void write(long value) throws OperationNotSupportedException {
        write(value, LONG_SIZE, this.buffer::putLong);
    }

    public void write(int value) throws OperationNotSupportedException {
        write(value, INT_SIZE, this.buffer::putInt);
    }

    public void write(short value) throws OperationNotSupportedException {
        write(value, SHORT_SIZE, this.buffer::putShort);
    }

    public void write(byte value) throws OperationNotSupportedException {
        write(value, BYTE_SIZE, this.buffer::put);
    }

    public void write(char value) throws OperationNotSupportedException {
        write(value, CHAR_SIZE, this.buffer::putChar);
    }

    public void write(boolean value) throws OperationNotSupportedException {
        write(value ? (byte)0xFF : (byte)0x00, BOOL_SIZE, this.buffer::put);
    }

    public void write(UUID uuid) throws OperationNotSupportedException {
        write(uuid.getMostSignificantBits());
        write(uuid.getLeastSignificantBits());
    }

    public void write(String value) throws OperationNotSupportedException {
        writeArray(value.getBytes(StandardCharsets.UTF_8));
    }

    public void writeArray(ByteBuffer buffer) throws OperationNotSupportedException {
        buffer.flip();

        ensureCanWrite(buffer.limit() + 4); // 4 is arr length (i32)

        write(buffer.limit());
        this.buffer.put(buffer);
    }

    public void writeArray(byte[] array) throws OperationNotSupportedException {
        if(array.length == 0) {
            write(0);
            return;
        }

        ensureCanWrite(array.length + 4); // 4 is arr length (i32)
        write(array.length);
        writeArrayWithoutLength(array);
    }

    public void writeArrayWithoutLength(byte[] array) throws OperationNotSupportedException {
        if(array.length == 0) {
            return;
        }

        ensureCanWrite(array.length);
        this.buffer.put(array);
    }

    public <T extends SerializableData> void write(T serializable) throws OperationNotSupportedException {
        ensureCanWrite(serializable.getSize());
        serializable.write(this);
    }

    public <T extends SerializableData> void writeArray(T[] serializableArray) throws OperationNotSupportedException {
        ensureCanWrite(BinaryProtocolUtils.sizeOf(serializableArray));
        write(serializableArray.length);

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

        if((this.buffer.capacity() - this.buffer.position()) < size) {
            resize(size);
        }
    }
    private void ensureCanWrite() throws OperationNotSupportedException {
        if(!canWrite) {
            throw new OperationNotSupportedException("Cannot use a closed packet writer");
        }
    }

    public ByteBuffer getBuffer() {
        close();
        return this.buffer;
    }

    @Override
    public void close() {
        this.canWrite = false;
    }
}
