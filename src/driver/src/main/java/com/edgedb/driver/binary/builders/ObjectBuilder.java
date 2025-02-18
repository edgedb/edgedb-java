package com.edgedb.driver.binary.builders;

import com.edgedb.driver.binary.builders.types.TypeBuilder;
import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.codecs.ObjectCodec;
import com.edgedb.driver.binary.codecs.visitors.TypeVisitor;
import com.edgedb.driver.clients.GelBinaryClient;
import com.edgedb.driver.exceptions.GelException;
import com.edgedb.driver.exceptions.NoTypeConverterException;
import com.edgedb.driver.util.TypeUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public final class ObjectBuilder {
    @FunctionalInterface
    public interface CollectionConverter<T extends Iterable<?>> {
        T convert(Object[] value);
    }

    private static final @NotNull Map<Class<?>, CollectionConverter<?>> collectionConverters;

    static {
        collectionConverters = new HashMap<>(){{
            put(List.class, List::of);
            put(HashSet.class, v -> new HashSet<>(List.of(v)));
        }};
    }

    public static <T> @Nullable T buildResult(@NotNull GelBinaryClient client, Codec<?> codec, @NotNull ByteBuf data, @NotNull Class<T> cls) throws GelException, OperationNotSupportedException {
        var visitor = new TypeVisitor(client);
        visitor.setTargetType(cls);
        codec = visitor.visit(codec);

        if(codec instanceof ObjectCodec) {
            return TypeBuilder.buildObject(client, cls, (ObjectCodec)codec, data);
        }

        var value = Codec.deserializeFromBuffer(codec, Objects.requireNonNull(data), client.getCodecContext());
        return convertTo(cls, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable T convertTo(@NotNull Class<T> cls, @Nullable Object value) throws GelException {
        try {
            if(value == null) {
                return null;
            }

            var valueType = value.getClass();

            if(cls.isAssignableFrom(valueType)) {
                return (T) value;
            }

            if(cls.isEnum() && value instanceof String) {
                //noinspection JavaReflectionInvocation
                return (T)cls.getMethod("valueOf", String.class).invoke(null, value);
            }

            if(Iterable.class.isAssignableFrom(cls)) {
                return convertCollection(cls, value);
            }

            if(valueType.isPrimitive() && TypeUtils.PRIMITIVE_REFERENCE_MAP.get(cls) == valueType) {
                return (T) value; // JVM handles the underlying conversions of primitives
            }

            if(
                    cls.isPrimitive() &&
                    TypeUtils.PRIMITIVE_REFERENCE_MAP.get(valueType) == cls
            ) {
                return (T) value; // JVM handles the underlying conversions of primitives
            }

            try {
                return cls.cast(value);
            } catch (Exception err) {
                throw new NoTypeConverterException(
                        String.format("Cannot use the type %s to represent the value %s", cls.getName(), valueType.getName()),
                        err
                );
            }
        } catch (Exception x) {
            throw new GelException("Failed to convert type to specified result", x);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertCollection(@NotNull Class<T> target, Object value) throws OperationNotSupportedException {
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
