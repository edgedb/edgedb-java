package com.gel.driver.binary.codecs;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.gel.driver.binary.protocol.common.descriptors.TypeOperation;
import com.gel.driver.exceptions.GelException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.Collection;
import java.util.UUID;

import static com.gel.driver.util.BinaryProtocolUtils.INT_SIZE;

public class CompoundCodec extends CodecBase<Object> {
    private final TypeOperation operation;
    private final Codec<?>[] innerCodecs;

    public CompoundCodec(
            UUID id,
            @Nullable CodecMetadata metadata,
            TypeOperation operation,
            Codec<?>[] elements
    ) {
        super(id, metadata, Object.class);

        this.operation = operation;
        this.innerCodecs = elements;
    }

    public TypeOperation getTypeOperation() {
        return this.operation;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void serialize(PacketWriter writer, @Nullable Object value, CodecContext context) throws OperationNotSupportedException, GelException {
        if(value == null) {
            writer.write(-1);
            return;
        }

        if(!(value instanceof Collection<?>)) {
            throw new IllegalArgumentException("The provided argument was not a collection");
        }


        var collection = ((Collection<?>)value).toArray();

        writer.write(collection.length);

        var visitor = context.getTypeVisitor();

        for(int i = 0; i != collection.length; i++) {
            writer.write(0); // reserved

            var elementValue = collection[i];

            if(elementValue == null) {
                writer.write(-1);
                continue;
            }

            visitor.setTargetType(elementValue.getClass());
            Codec codec = visitor.visit(this.innerCodecs[i]);
            visitor.reset();

            writer.writeDelegateWithLength((v) -> codec.serialize(v, elementValue, context));
        }
    }

    @Nullable
    @Override
    public Object deserialize(PacketReader reader, CodecContext context) throws GelException, OperationNotSupportedException {
        var numElements = reader.readInt32();

        if(numElements != this.innerCodecs.length) {
            throw new GelException("Expected " + this.innerCodecs.length + " elements, but got " + numElements);
        }

        var elements = new Object[numElements];

        for(var i = 0; i != numElements; i++) {
            reader.skip(INT_SIZE);

            try(var elementReader = reader.scopedSlice()) {
                elements[i] = innerCodecs[i].deserialize(elementReader, context);
            }
        }

        return elements;
    }
}
