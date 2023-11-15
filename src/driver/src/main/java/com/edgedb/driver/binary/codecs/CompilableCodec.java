package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.protocol.common.descriptors.CodecMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public final class CompilableCodec implements Codec {
    @FunctionalInterface
    public interface CompilableFactory {
        Codec<?> compile(UUID id, @Nullable CodecMetadata metadata, Class<?> cls, Codec<?> innerCodec);
    }

    private final Codec<?> innerCodec;
    private final CompilableFactory factory;
    private final @NotNull ConcurrentMap<Class<?>, Codec<?>> instanceCache;
    private final Function<Class<?>, Class<?>> compilableTypeFactory;
    private final UUID id;
    private final @Nullable CodecMetadata metadata;

    private @Nullable Class<?> compilableType;

    public CompilableCodec(
            UUID id,
            @Nullable CodecMetadata metadata,
            Codec<?> innerCodec,
            CompilableFactory factory,
            Function<Class<?>, Class<?>> compilableTypeFactory
    ) {
        this.id = id;
        this.metadata = metadata;
        this.factory = factory;
        this.innerCodec = innerCodec;
        this.instanceCache = new ConcurrentHashMap<>();
        this.compilableTypeFactory = compilableTypeFactory;
    }

    public Class<?> getCompilableType() {
        if(compilableType != null) {
            return compilableType;
        }

        return compilableType = compilableTypeFactory.apply(getInnerType());
    }

    public Codec<?> getInnerCodec() {
        return this.innerCodec;
    }

    public Codec<?> getOrCompile(Class<?> cls, Codec<?> innerCodec) {
        return instanceCache.computeIfAbsent(cls, (c) -> compile(c, innerCodec));
    }

    public Codec<?> compile(Class<?> cls, Codec<?> innerCodec) {
        return this.factory.compile(this.id, this.metadata, cls, innerCodec);
    }

    public Class<?> getInnerType() {
        return this.innerCodec instanceof CompilableCodec
                ? ((CompilableCodec)this.innerCodec).getCompilableType()
                : this.innerCodec.getConvertingClass();
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public @Nullable CodecMetadata getMetadata() {
        return this.metadata;
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Object value, CodecContext context) throws OperationNotSupportedException {
        throw new OperationNotSupportedException();
    }

    @Nullable
    @Override
    public Object deserialize(PacketReader reader, CodecContext context) throws OperationNotSupportedException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Class<?> getConvertingClass() {
        throw new RuntimeException(new OperationNotSupportedException());
    }

    @Override
    public boolean canConvert(Type type) {
        return false;
    }
}
