package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.builders.ObjectEnumerator;
import com.edgedb.driver.binary.builders.TypeDeserializerFactory;
import com.edgedb.driver.binary.builders.types.TypeDeserializerInfo;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.Map;

@SuppressWarnings("rawtypes")
public final class ObjectCodec extends CodecBase<Object> implements ArgumentCodec<Object> {

    public final Codec[] innerCodecs;
    public final String[] propertyNames;
    private TypeDeserializerFactory<?> factory;

    private final Object lock = new Object();

    public ObjectCodec(Codec[] innerCodecs, String[] propertyNames) {
        super(Object.class);
        this.innerCodecs = innerCodecs;
        this.propertyNames = propertyNames;
    }

    public void initialize(Class<?> cls) {
        // TODO: get the deserialization factory for 'cls
        if(cls.equals(Object.class)) {
            factory = ObjectEnumerator::flatten;
        }
    }

    public void initialize(TypeDeserializerInfo info) {

    }

    @SuppressWarnings("unchecked")
    @Override
    public void serializeArguments(PacketWriter writer, @Nullable Map<String, ?> value, CodecContext context) throws EdgeDBException, OperationNotSupportedException {
        if(value == null) {
            throw new EdgeDBException("Arguments cannot be null");
        }

        writer.write(value.size());

        var visitor = context.getTypeVisitor();

        for(int i = 0; i != value.size(); i++) {
            var propName = this.propertyNames[i];

            if(!value.containsKey(propName)) {
                writer.write(-1);
                continue;
            }

            var element = value.get(propName);

            if(element == null) {
                writer.write(-1);
                continue;
            }

            visitor.setTargetType(element.getClass());
            var codec = (Codec)visitor.visit(this.innerCodecs[i]);
            visitor.reset();


            // TODO: codec visitor
            writer.writeDelegateWithLength((v) -> codec.serialize(v, element, context));
        }
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Object value, CodecContext context) throws OperationNotSupportedException {
        throw new OperationNotSupportedException();
    }

    @Override
    public @Nullable Object deserialize(PacketReader reader, CodecContext context) throws EdgeDBException {
        synchronized (lock) {
            if(factory == null) {
                initialize(Object.class);
            }
        }

        var enumerator = new ObjectEnumerator(reader, this, context);

        try {
            return factory.deserialize(enumerator);
        } catch (Exception x) {
            throw new EdgeDBException("Failed to deserialize object to " + getConvertingClass().getName(), x);
        }
    }
}
