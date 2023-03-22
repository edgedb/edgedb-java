package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.function.BiFunction;

@SuppressWarnings("rawtypes")
public final class CompilableCodec implements Codec {

    private final UUID id;
    private final Codec<?> innerCodec;
    private final BiFunction<Class<?>, Codec<?>, Codec<?>> factory;

    public CompilableCodec(UUID id, Codec<?> innerCodec, BiFunction<Class<?>, Codec<?>, Codec<?>> factory) {
        this.factory = factory;
        this.id = id;
        this.innerCodec = innerCodec;
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Object value, CodecContext context) throws OperationNotSupportedException, EdgeDBException {
        throw new OperationNotSupportedException();
    }

    @Nullable
    @Override
    public Object deserialize(PacketReader reader, CodecContext context) throws EdgeDBException, OperationNotSupportedException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Class<?> getConvertingClass() {
        throw new OperationNotSupportedException();
    }

    @Override
    public boolean canConvert(Type type) {
        return false;
    }
}
