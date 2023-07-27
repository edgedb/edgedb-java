package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.datatypes.Tuple;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

import java.util.UUID;

import static com.edgedb.driver.util.BinaryProtocolUtils.INT_SIZE;

public final class TupleCodec extends CodecBase<Tuple> {
    public final Codec<?>[] innerCodecs;

    public TupleCodec(UUID id, Codec<?>[] codecs) {
        super(id, Tuple.class);
        this.innerCodecs = codecs;
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Tuple value, CodecContext context) throws OperationNotSupportedException, EdgeDBException {
        if(value == null || value.size() == 0) {
            writer.write(-1);
            return;
        }

        if(innerCodecs.length != value.size()) {
            throw new IllegalArgumentException("Tuple length does not match descriptor length");
        }

        writer.write(value.size());

        for(int i = 0; i != value.size(); i++) {
            //noinspection unchecked
            var codec = (Codec<Object>)innerCodecs[i];
            var element = value.get(i, codec.getConvertingClass());

            writer.write(0); // reserved

            if(element == null) {
                writer.write(-1);
            } else {
                writer.writeDelegateWithLength((w) -> codec.serialize(w, element, context));
            }
        }
    }

    @Override
    public @NotNull Tuple deserialize(@NotNull PacketReader reader, CodecContext context) throws EdgeDBException, OperationNotSupportedException {
        var numElements = reader.readInt32();

        if(innerCodecs.length != numElements) {
            throw new EdgeDBException("Codec count does not match number of elements in tuple");
        }

        Tuple.Element[] elements = new Tuple.Element[numElements];

        for(int i = 0; i != numElements; i++) {
            var codec = innerCodecs[i];

            reader.skip(INT_SIZE); // reserved

            var data = reader.readByteArray();

            if(data == null) {
                elements[i] = Tuple.Element.of(null, codec.getConvertingClass());
                continue;
            }

            elements[i] = Tuple.Element.of(codec.deserialize(new PacketReader(data), context), codec.getConvertingClass());
        }

        return Tuple.of(elements);
    }
}
