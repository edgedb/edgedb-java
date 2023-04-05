package com.edgedb.binary.codecs;

import com.edgedb.binary.PacketReader;
import com.edgedb.binary.PacketWriter;
import com.edgedb.datatypes.Tuple;
import com.edgedb.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

import static com.edgedb.util.BinaryProtocolUtils.INT_SIZE;

public final class TupleCodec extends CodecBase<Tuple> {
    private final Codec<?>[] innerCodecs;

    public TupleCodec(Codec<?>[] codecs) {
        super(Tuple.class);
        this.innerCodecs = codecs;
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Tuple value, CodecContext context) throws OperationNotSupportedException, EdgeDBException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Tuple deserialize(PacketReader reader, CodecContext context) throws EdgeDBException, OperationNotSupportedException {
        var numElements = reader.readInt32();

        if(innerCodecs.length != numElements) {
            throw new EdgeDBException("Codec count does not match number of elements in tuple");
        }

        Tuple.TupleElement[] elements = new Tuple.TupleElement[numElements];

        for(int i = 0; i != numElements; i++) {
            var codec = innerCodecs[i];

            reader.skip(INT_SIZE); // reserved

            var data = reader.readByteArray();

            if(data == null) {
                elements[i] = new Tuple.TupleElement(codec.getConvertingClass(), null);
                continue;
            }

            elements[i] = new Tuple.TupleElement(codec.getConvertingClass(), codec.deserialize(new PacketReader(data), context));
        }

        return new Tuple(elements);
    }
}
