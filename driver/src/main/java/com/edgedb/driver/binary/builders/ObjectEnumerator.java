package com.edgedb.driver.binary.builders;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.codecs.ObjectCodec;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.HashMap;
import java.util.Map;

import static com.edgedb.driver.util.BinaryProtocolUtils.INT_SIZE;

public final class ObjectEnumerator {

    private final PacketReader reader;
    private final CodecContext context;
    private final ObjectCodec codec;

    private final int numElements;
    private int position;

    public ObjectEnumerator(PacketReader reader, ObjectCodec codec, CodecContext context) {
        this.reader = reader;
        this.context = context;
        this.codec = codec;

        this.numElements = reader.readInt32();
    }

    public boolean hasRemaining() {
        return position < numElements && !reader.isEmpty();
    }

    public @Nullable ObjectElement next() throws EdgeDBException, OperationNotSupportedException {
        if(!hasRemaining()) {
            return null;
        }

        try {
            reader.skip(INT_SIZE);

            var name = codec.propertyNames[position];
            var elementCodec = codec.innerCodecs[position];

            var data = reader.readByteArray();

            if(data == null || data.readableBytes() == 0) {
                return new ObjectElement(name, null, elementCodec.getConvertingClass());
            }

            return new ObjectElement(
                    name,
                    elementCodec.deserialize(new PacketReader(data), context),
                    elementCodec.getConvertingClass()
            );
        }
        finally {
            position++;
        }
    }

    public Map<String, Object> flatten() throws EdgeDBException, OperationNotSupportedException {
        return new HashMap<>() {
            {
                ObjectElement element;
                while(hasRemaining() && (element = next()) != null) {
                    put(element.name, element.value);
                }
            }
        };
    }

    public static final class ObjectElement {
        public final String name;
        public final Object value;
        public final Class<?> type;

        public ObjectElement(String name, Object value, Class<?> type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }
    }
}
