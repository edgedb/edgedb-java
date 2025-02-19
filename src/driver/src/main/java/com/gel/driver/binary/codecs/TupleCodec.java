package com.gel.driver.binary.codecs;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.gel.driver.datatypes.Tuple;
import com.gel.driver.exceptions.GelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

import java.util.UUID;

import static com.gel.driver.util.BinaryProtocolUtils.INT_SIZE;

public final class TupleCodec extends CodecBase<Tuple> {
    public final Codec<?>[] innerCodecs;

    public TupleCodec(UUID id, @Nullable CodecMetadata metadata, Codec<?>[] codecs) {
        super(id, metadata, Tuple.class);
        this.innerCodecs = codecs;
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Tuple value, CodecContext context) throws OperationNotSupportedException, GelException {
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
    public @NotNull Tuple deserialize(@NotNull PacketReader reader, CodecContext context) throws GelException, OperationNotSupportedException {
        var numElements = reader.readInt32();

        if(innerCodecs.length != numElements) {
            throw new GelException("Codec count does not match number of elements in tuple");
        }

        Tuple.Element[] elements = new Tuple.Element[numElements];

        for(int i = 0; i != numElements; i++) {
            var codec = innerCodecs[i];

            reader.skip(INT_SIZE); // reserved

            try(var elementReader = reader.scopedSlice()) {
                if(elementReader.isNoData) {
                    elements[i] = Tuple.Element.of(null, codec.getConvertingClass());
                    continue;
                }

                elements[i] = Tuple.Element.of(codec.deserialize(elementReader, context), codec.getConvertingClass());
            }


        }

        return Tuple.of(elements);
    }
}
