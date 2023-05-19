package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.builders.internal.ObjectEnumeratorImpl;
import com.edgedb.driver.binary.builders.types.TypeBuilder;
import com.edgedb.driver.binary.builders.types.TypeDeserializerInfo;
import com.edgedb.driver.binary.descriptors.common.ShapeElement;
import com.edgedb.driver.binary.descriptors.common.TupleElement;
import com.edgedb.driver.binary.packets.shared.Cardinality;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public class ObjectCodec extends CodecBase<Object> implements ArgumentCodec<Object> {

    public static final class TypeInitializedObjectCodec extends ObjectCodec {
        private final TypeDeserializerInfo<?> deserializer;
        private final Class<?> target;
        private final ObjectCodec parent;

        public TypeInitializedObjectCodec(Class<?> target, ObjectCodec parent) throws EdgeDBException {
            super(parent.elements);

            this.parent = parent;
            this.target = target;
            this.deserializer = TypeBuilder.getDeserializerInfo(target);

            if(this.deserializer == null) {
                // TODO: make NoTypeConverterException class
                throw new EdgeDBException("Failed to find type deserializer for " + target.getName());
            }
        }

        public TypeInitializedObjectCodec(TypeDeserializerInfo<?> info, ObjectCodec parent) {
            super(parent.elements);
            assert info != null;

            this.parent = parent;
            this.target = info.getType();
            this.deserializer = info;
        }

        @Override
        public @Nullable Object deserialize(PacketReader reader, CodecContext context) throws EdgeDBException {
            assert deserializer != null;

            var enumerator = new ObjectEnumeratorImpl(reader, this, context);

            try {
                return deserializer.factory.deserialize(enumerator);
            } catch (Exception x) {
                throw new EdgeDBException("Failed to deserialize object to " + getConvertingClass().getName(), x);
            }
        }

        public Class<?> getTarget() {
            return target;
        }

        public ObjectCodec getParent() {
            return parent;
        }

        public TypeDeserializerInfo<?> getDeserializer() {
            return deserializer;
        }
    }

    public static final class Element {
        public String name;
        public Codec<?> codec;
        public @Nullable Cardinality cardinality;
        public Element(String name, Codec<?> codec, @Nullable Cardinality cardinality) {
            this.name = name;
            this.codec = codec;
            this.cardinality = cardinality;
        }
    }

    public final Element[] elements;
    private final ConcurrentMap<Class<?>, TypeInitializedObjectCodec> typeCodecs;
    private final Object lock = new Object();

    public ObjectCodec(Element... elements) {
        super(Object.class);
        this.elements = elements;
        this.typeCodecs = new ConcurrentHashMap<>();
    }

    public static ObjectCodec create(Function<Integer, Codec<?>> fetchCodec, ShapeElement[] shape) {
        var elements = new Element[shape.length];

        for (int i = 0; i < shape.length; i++) {
            var shapeElement = shape[i];

            elements[i] = new Element(
                    shapeElement.name,
                    fetchCodec.apply(shapeElement.typePosition.intValue()),
                    shapeElement.cardinality
            );
        }

        return new ObjectCodec(elements);
    }

    public static ObjectCodec create(Function<Short, Codec<?>> fetchCodec, TupleElement[] shape) {
        var elements = new Element[shape.length];

        for (int i = 0; i < shape.length; i++) {
            var shapeElement = shape[i];

            elements[i] = new Element(
                    shapeElement.name,
                    fetchCodec.apply(shapeElement.typePosition),
                    null
            );
        }

        return new ObjectCodec(elements);
    }

    public TypeInitializedObjectCodec getOrCreateTypeCodec(Class<?> cls) throws EdgeDBException {
        return getOrCreateTypeCodec(cls, t -> new TypeInitializedObjectCodec(t, this));
    }
    public TypeInitializedObjectCodec getOrCreateTypeCodec(TypeDeserializerInfo<?> info) throws EdgeDBException {
        return getOrCreateTypeCodec(info.getType(), t -> new TypeInitializedObjectCodec(info, this));
    }

    @FunctionalInterface
    private interface TypeInitializedCodecFactory {
        TypeInitializedObjectCodec construct(Class<?> cls) throws EdgeDBException;
    }
    private TypeInitializedObjectCodec getOrCreateTypeCodec(Class<?> cls, TypeInitializedCodecFactory factory) throws EdgeDBException {
        try {
            return typeCodecs.computeIfAbsent(cls, t -> {
                try {
                    return factory.construct(t);
                } catch (EdgeDBException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        catch (RuntimeException e) {
            if(e.getCause() instanceof EdgeDBException) {
                throw (EdgeDBException)e.getCause();
            }

            throw e;
        }
    }

    @Override
    public final void serializeArguments(PacketWriter writer, @Nullable Map<String, ?> value, CodecContext context) throws EdgeDBException, OperationNotSupportedException {
        this.serialize(writer, value, context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void serialize(PacketWriter writer, @Nullable Object rawValue, CodecContext context) throws OperationNotSupportedException, EdgeDBException {
        if(rawValue == null) {
            throw new IllegalArgumentException("Serializable object value cannot be null");
        }

        if(!(rawValue instanceof Map)) {
            throw new EdgeDBException("Expected map type for object serialization");
        }

        var value = (Map<String, ?>)rawValue;

        writer.write(value.size());

        var visitor = context.getTypeVisitor();

        for(int i = 0; i != value.size(); i++) {
            var element = this.elements[i];

            writer.write(0); // reserved

            if(!value.containsKey(element.name)) {
                writer.write(-1);
                continue;
            }

            var elementValue = value.get(element.name);

            if(elementValue == null) {
                writer.write(-1);
                continue;
            }

            visitor.setTargetType(elementValue.getClass());
            var codec = (Codec)visitor.visit(element.codec);
            visitor.reset();

            writer.writeDelegateWithLength((v) -> codec.serialize(v, elementValue, context));
        }
    }

    @Override
    public @Nullable Object deserialize(PacketReader reader, CodecContext context) throws EdgeDBException {
        var enumerator = new ObjectEnumeratorImpl(reader, this, context);

        try {
            return enumerator.flatten();
        } catch (Exception x) {
            throw new EdgeDBException("Failed to deserialize object to " + getConvertingClass().getName(), x);
        }
    }
}
