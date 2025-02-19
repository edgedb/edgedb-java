package com.gel.driver.binary.codecs.scalars.complex;

import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.codecs.ComplexCodec;
import com.gel.driver.binary.codecs.RuntimeCodec;
import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.codecs.CodecContext;
import com.gel.driver.binary.codecs.complex.ComplexCodecBase;
import com.gel.driver.binary.codecs.complex.ComplexCodecConverter;
import com.gel.driver.binary.codecs.scalars.ScalarCodec;
import com.gel.driver.binary.codecs.scalars.ScalarCodecBase;
import com.gel.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.gel.driver.exceptions.GelException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.UUID;

public abstract class ComplexScalarCodecBase<T> extends ComplexCodecBase<T> implements ScalarCodec<T> {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ComplexScalarCodecBase(UUID id, @Nullable CodecMetadata metadata, Class<T> cls, ComplexCodecConverter<T, ?>... converters) {
        super(id, metadata, cls, (c, p, cv) -> new RuntimeScalarCodecImpl(c, p, cv), converters);
    }

}

final class RuntimeScalarCodecImpl<T, U> extends ScalarCodecBase<U> implements RuntimeCodec<U> {
    private final ComplexCodecBase<T> parent;
    private final ComplexCodecConverter<T, U> converter;


    public RuntimeScalarCodecImpl(Class<U> cls, ComplexCodecBase<T> parent, ComplexCodecConverter<T, U> converter) {
        super(parent.id, parent.metadata, cls);
        this.parent = parent;
        this.converter = converter;
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable U value, CodecContext context) throws OperationNotSupportedException, GelException {
        var converted = value == null ? null : converter.from.apply(value);
        this.parent.serialize(writer, converted, context);
    }

    @Override
    public @Nullable U deserialize(PacketReader reader, CodecContext context) throws GelException, OperationNotSupportedException {
        var value = parent.deserialize(reader, context);
        return value == null ? null : converter.to.apply(value);
    }

    @Override
    public ComplexCodec<?> getBroker() {
        return this.parent;
    }
}
