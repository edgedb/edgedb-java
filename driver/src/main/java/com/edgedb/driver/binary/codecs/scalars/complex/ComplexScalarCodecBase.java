package com.edgedb.driver.binary.codecs.scalars.complex;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.codecs.ComplexCodec;
import com.edgedb.driver.binary.codecs.RuntimeCodec;
import com.edgedb.driver.binary.codecs.complex.ComplexCodecBase;
import com.edgedb.driver.binary.codecs.complex.ComplexCodecConverter;
import com.edgedb.driver.binary.codecs.scalars.ScalarCodec;
import com.edgedb.driver.binary.codecs.scalars.ScalarCodecBase;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public abstract class ComplexScalarCodecBase<T> extends ComplexCodecBase<T> implements ScalarCodec<T> {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ComplexScalarCodecBase(Class<T> cls, ComplexCodecConverter<T, ?>... converters) {
        super(cls, (c, p, cv) -> new RuntimeScalarCodecImpl(c, p, cv), converters);
    }

}

final class RuntimeScalarCodecImpl<T, U> extends ScalarCodecBase<U> implements RuntimeCodec<U> {
    private final ComplexCodecBase<T> parent;
    private final ComplexCodecConverter<T, U> converter;


    public RuntimeScalarCodecImpl(Class<U> cls, ComplexCodecBase<T> parent, ComplexCodecConverter<T, U> converter) {
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
