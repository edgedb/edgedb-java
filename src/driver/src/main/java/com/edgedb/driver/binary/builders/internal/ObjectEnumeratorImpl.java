package com.edgedb.driver.binary.builders.internal;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.ObjectEnumerator;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.codecs.ObjectCodec;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.HashMap;
import java.util.Map;

import static com.edgedb.driver.util.BinaryProtocolUtils.INT_SIZE;

public final class ObjectEnumeratorImpl implements ObjectEnumerator {
    private final @NotNull PacketReader reader;
    private final CodecContext context;
    private final ObjectCodec codec;

    private final int numElements;
    private int position;

    public ObjectEnumeratorImpl(@NotNull PacketReader reader, ObjectCodec codec, CodecContext context) {
        this.reader = reader;
        this.context = context;
        this.codec = codec;

        this.numElements = reader.readInt32();
    }

    public EdgeDBBinaryClient getClient() {
        return this.context.client;
    }

    @Override
    public boolean hasRemaining() {
        return position < numElements && !reader.isEmpty();
    }

    @Override
    public @Nullable ObjectEnumerator.ObjectElement next() throws EdgeDBException, OperationNotSupportedException {
        if(!hasRemaining()) {
            return null;
        }

        try {
            reader.skip(INT_SIZE);

            var element = codec.elements[position];

            var data = reader.readByteArray();

            if(data == null || data.readableBytes() == 0) {
                return new ObjectEnumerator.ObjectElement(element.name, null, element.codec.getConvertingClass());
            }

            return new ObjectEnumerator.ObjectElement(
                    element.name,
                    element.codec.deserialize(new PacketReader(data), context),
                    element.codec.getConvertingClass()
            );
        }
        finally {
            position++;
        }
    }

    @Override
    public @NotNull Map<String, Object> flatten() throws EdgeDBException, OperationNotSupportedException {
        return new HashMap<>() {
            {
                ObjectEnumerator.ObjectElement element;
                while(hasRemaining() && (element = next()) != null) {
                    put(element.getName(), element.getValue());
                }
            }
        };
    }
}