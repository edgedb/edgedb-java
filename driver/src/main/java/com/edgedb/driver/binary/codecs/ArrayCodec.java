package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Array;

import static com.edgedb.driver.util.BinaryProtocolUtils.LONG_SIZE;

public class ArrayCodec<T> extends CodecBase<T[]> {
    private static final byte[] EMPTY_ARRAY = new byte[] {
            0,0,0,0,
            0,0,0,0,
            0,0,0,0,
            0,0,0,0,
            0,0,0,1
    };

    private final Codec<T> innerCodec;

    @SuppressWarnings("unchecked")
    public ArrayCodec(Codec<T> codec) {
        super((Class<T[]>)Array.newInstance(codec.getConvertingClass(), 0).getClass()); // TODO: can this be done better?
        this.innerCodec = codec;
    }

    @Override
    public void serialize(PacketWriter writer, T @Nullable [] value, CodecContext context) throws OperationNotSupportedException, EdgeDBException {
        if(value == null) {
            writer.writeArrayWithoutLength(EMPTY_ARRAY);
            return;
        }

        writer.write(1); // num dimensions
        writer.write(0); // reserved
        writer.write(0); // reserved

        // dimensions: length for upper, 1 for lower
        writer.write(value.length);
        writer.write(1);

        for(int i = 0; i != value.length; i++) {
            var element = value[i];

            if(element == null) {
                writer.write(-1);
            } else {
                writer.writeDelegateWithLength((w) -> this.innerCodec.serialize(w, element, context));
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T @Nullable [] deserialize(PacketReader reader, CodecContext context) throws EdgeDBException {
        var dimensions = reader.readInt32();

        reader.skip(LONG_SIZE); // reserved

        if(dimensions == 0) {
            return (T[])Array.newInstance(innerCodec.getConvertingClass(), 0);
        }

        var upper = reader.readInt32();
        var lower = reader.readInt32();

        var numElements = upper - lower + 1;

        var array = (T[])Array.newInstance(innerCodec.getConvertingClass(), numElements);

        for(int i = 0; i != numElements; i++) {
            // TODO: memory falloff here? the buff returned here should have shared lifetime with the
            // buff in the root packet reader
            var data = reader.readByteArray();
            array[i] = innerCodec.deserialize(new PacketReader(data), context);
        }

        return array;
    }
}