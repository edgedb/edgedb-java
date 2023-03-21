package com.edgedb.driver.binary.codecs.complex;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.*;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public abstract class ComplexCodecBase<T> extends CodecBase<T> implements ComplexCodec<T> {
    private final Map<Class<?>, Codec<?>> runtimeCodecs;
    private final Map<Class<?>, ComplexCodecConverter<T, ?>> converters;
    protected final RuntimeCodecFactory runtimeFactory;

    @SafeVarargs
    public ComplexCodecBase(Class<T> cls, ComplexCodecConverter<T, ?>... converters) {
        this(cls, null, converters);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @SafeVarargs
    public ComplexCodecBase(Class<T> cls, @Nullable RuntimeCodecFactory runtimeFactory, ComplexCodecConverter<T, ?>... converters) {
        super(cls);

        this.runtimeFactory = runtimeFactory == null
                ? (cls1, parent, converter) -> new RuntimeCodecImpl(cls1, parent, converter)
                : runtimeFactory;

        this.runtimeCodecs = new HashMap<>(converters.length);
        this.converters = Arrays.stream(converters).collect(Collectors.toMap(x -> x.targetType, x -> x));
    }

    @Override
    public void buildRuntimeCodecs() {
        if(converters == null || runtimeCodecs.size() == converters.size()) {
            return;
        }

        for(var converter : this.converters.entrySet()) {
            var codec = runtimeFactory.create(converter.getKey(), this, converter.getValue()); // TODO: cache instance
            runtimeCodecs.put(converter.getKey(), codec);
        }
    }

    @Override
    public Codec<?> getCodecFor(Class<?> type) {
        if(this.canConvert(type)) {
            return this;
        }

        if(runtimeCodecs.containsKey(type)) {
            return runtimeCodecs.get(type);
        }

        throw new MissingResourceException(
                "Cannot find implementation codec",
                "RuntimeCodecImpl",
                type.getName()
        );
    }

    @Override
    public boolean canConvert(Type type) {
        return super.canConvert(type) || (type instanceof Class<?> && runtimeCodecs.containsKey((Class<?>)type));
    }

    @Override
    public Collection<Codec<?>> getRuntimeCodecs() {
        return runtimeCodecs.values();
    }

    private final class RuntimeCodecImpl<U> extends CodecBase<U> implements RuntimeCodec<U> {
        private final ComplexCodecBase<T> parent;
        private final ComplexCodecConverter<T, U> converter;


        public RuntimeCodecImpl(Class<U> cls, ComplexCodecBase<T> parent, ComplexCodecConverter<T, U> converter) {
            super(cls);
            this.parent = parent;
            this.converter = converter;
        }

        @Override
        public void serialize(PacketWriter writer, @Nullable U value, CodecContext context) throws OperationNotSupportedException {
            var converted = value == null ? null : converter.from.apply(value);
            this.parent.serialize(writer, converted, context);
        }

        @Override
        public @Nullable U deserialize(PacketReader reader, CodecContext context) {
            var value = parent.deserialize(reader, context);
            return value == null ? null : converter.to.apply(value);
        }

        @Override
        public ComplexCodec<?> getBroker() {
            return this.parent;
        }
    }
}
