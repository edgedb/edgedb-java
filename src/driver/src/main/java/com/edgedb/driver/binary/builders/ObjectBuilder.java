package com.edgedb.driver.binary.builders;

import com.edgedb.driver.binary.builders.types.TypeBuilder;
import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.codecs.ObjectCodec;
import com.edgedb.driver.binary.codecs.visitors.TypeVisitor;
import com.edgedb.driver.binary.packets.receivable.Data;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.exceptions.EdgeDBException;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public final class ObjectBuilder {

    @FunctionalInterface
    public interface CollectionConverter<T extends Iterable<?>> {
        T convert(Object[] value);
    }

    private static final Map<Class<?>, CollectionConverter<?>> collectionConverters;

    static {
        collectionConverters = new HashMap<>(){{
            put(List.class, List::of);
            put(HashSet.class, v -> new HashSet<>(List.of(v)));
        }};
    }

    public static <T> T buildResult(EdgeDBBinaryClient client, Codec<?> codec, Data data, Class<T> cls) throws EdgeDBException, OperationNotSupportedException {
        var visitor = new TypeVisitor(client);
        visitor.setTargetType(cls);
        codec = visitor.visit(codec);

        if(codec instanceof ObjectCodec) {
            // TODO: type builder
            return TypeBuilder.buildObject(client, cls, (ObjectCodec)codec, data);
        }

        var value = Codec.deserializeFromBuffer(codec, data.payloadBuffer, client.getCodecContext());
        return convertTo(cls, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T convertTo(Class<T> cls, Object value) throws EdgeDBException {
        try {
            if(value == null) {
                return null;
            }

            var valueType = value.getClass();

            if(cls.isAssignableFrom(valueType)) {
                return (T) value;
            }

            if(cls.isEnum() && value instanceof String) {
                return (T)cls.getMethod("valueOf", String.class).invoke(null, (String) value);
            }

            if(Iterable.class.isAssignableFrom(cls)) {
                return convertCollection(cls, value);
            }

            return (T)value;
        }
        catch (Exception x) {
            throw new EdgeDBException("Failed to convert type to specified result", x);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertCollection(Class<T> target, Object value) throws OperationNotSupportedException {
        if(!(value instanceof Object[])) {
            throw new IllegalArgumentException("Value is not a collection");
        }

        if(collectionConverters.containsKey(target)) {
            return (T)collectionConverters.get(target).convert((Object[])value);
        }

        // slow check
        for (var converter : collectionConverters.entrySet()) {
            if(target.isAssignableFrom(converter.getKey())) {
                return (T)converter.getValue().convert((Object[])value);
            } else if (converter.getKey().isAssignableFrom(target)) {
                // check for ctor with collection param
                try {
                    var ctor = target.getConstructor(Collection.class);
                    //noinspection JavaReflectionInvocation
                    return ctor.newInstance(converter.getValue().convert((Object[])value));
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException ignored) {
                }
            }
        }

        throw new OperationNotSupportedException("Cannot convert Object[] to target collection type " + target.getName());
    }
}
