package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.builders.ObjectEnumerator;
import com.edgedb.driver.binary.builders.TypeDeserializerFactory;
import com.edgedb.driver.binary.builders.types.TypeBuilder;
import com.edgedb.driver.binary.builders.types.TypeDeserializerInfo;
import com.edgedb.driver.binary.descriptors.common.ShapeElement;
import com.edgedb.driver.binary.packets.shared.Cardinality;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public final class ObjectCodec extends CodecBase<Object> implements ArgumentCodec<Object> {
    public static final class Element {
        public String name;
        public Codec<?> codec;
        public Cardinality cardinality;
        public Element(String name, Codec<?> codec, Cardinality cardinality) {
            this.name = name;
            this.codec = codec;
            this.cardinality = cardinality;
        }
    }

    public final Element[] elements;
    private TypeDeserializerFactory<?> factory;
    private @Nullable TypeDeserializerInfo<?> deserializerInfo;
    private @Nullable Class<?> initializedType;

    private final Object lock = new Object();

    public ObjectCodec(Element... elements) {
        super(Object.class);
        this.elements = elements;
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

    public @Nullable TypeDeserializerInfo<?> getDeserializerInfo() {
        return this.deserializerInfo;
    }

    public void initialize(Class<?> cls) {
        if(cls.equals(initializedType)) {
           return;
        }

        if(cls.equals(Object.class)) {
            factory = (e, p) -> e.flatten();
            return;
        }

        this.deserializerInfo = TypeBuilder.getDeserializerInfo(cls);

        if(this.deserializerInfo != null) {
            this.factory = this.deserializerInfo.factory;
        }

        this.initializedType = cls;
    }

    public void initialize(TypeDeserializerInfo info) {
        this.factory = info.factory;
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
            var element = this.elements[i];

            if(!value.containsKey(element.name)) {
                writer.write(-1);
                continue;
            }

            var elementValue = value.get(element.name);

            if(elementValue == null) {
                writer.write(-1);
                continue;
            }

            visitor.setTargetType(element.getClass());
            var codec = (Codec)visitor.visit(element.codec);
            visitor.reset();

            writer.writeDelegateWithLength((v) -> codec.serialize(v, elementValue, context));
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
