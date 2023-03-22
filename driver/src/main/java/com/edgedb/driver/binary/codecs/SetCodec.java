package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static com.edgedb.driver.util.BinaryProtocolUtils.INT_SIZE;
import static com.edgedb.driver.util.BinaryProtocolUtils.LONG_SIZE;

public final class SetCodec<T> extends CodecBase<Collection<T>> {
    private final Codec<T> innerCodec;

    public SetCodec(Class<Collection<T>> cls, Codec<T> innerCodec) {
        super(cls);
        this.innerCodec = innerCodec;
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Collection<T> value, CodecContext context) throws OperationNotSupportedException {
        throw new OperationNotSupportedException();
    }

    @Override
    public @Nullable Collection<T> deserialize(PacketReader reader, CodecContext context) throws EdgeDBException {
        return deserializeSet(
                reader,
                context,
                innerCodec instanceof ArrayCodec<?>
                        ? this::deserializeEnvelopeElement
                        : this::deserializeSetElement
        );
    }

    @SuppressWarnings("unchecked")
    private Collection<T> deserializeSet(PacketReader reader, CodecContext context, SetElementDeserializer<T> elementDeserializer) throws EdgeDBException {
        var dimensions = reader.readInt32();

        reader.skip(LONG_SIZE); // flags & reserved

        if(dimensions == 0) {
            return Set.of();
        }

        if(dimensions != 1) {
            throw new EdgeDBException("Only dimensions of 1 are supported for sets");
        }

        var upper = reader.readInt32();
        var lower = reader.readInt32();

        var numElements = upper - lower + 1;

        var result = (T[])Array.newInstance(innerCodec.getConvertingClass(), numElements);

        for(int i = 0; i != numElements; i++) {
            result[i] = elementDeserializer.deserialize(reader, context);
        }

        return Arrays.asList(result);
    }

    private @Nullable T deserializeEnvelopeElement(PacketReader reader, CodecContext context) throws EdgeDBException {
        reader.skip(INT_SIZE);

        var envelopeElements = reader.readInt32();

        if(envelopeElements != 1) {
            throw new EdgeDBException(String.format("Envelope should contain only one element, but this envelope contains %d", envelopeElements));
        }

        reader.skip(INT_SIZE);

        return innerCodec.deserialize(reader, context);
    }

    private @Nullable T deserializeSetElement(PacketReader reader, CodecContext context) throws EdgeDBException {
        var data = reader.readByteArray();

        if(data == null) {
            return null;
        }

        return innerCodec.deserialize(new PacketReader(data), context);
    }

    @FunctionalInterface
    private interface SetElementDeserializer<T> {
        @Nullable T deserialize(PacketReader reader, CodecContext context) throws EdgeDBException;
    }
}
