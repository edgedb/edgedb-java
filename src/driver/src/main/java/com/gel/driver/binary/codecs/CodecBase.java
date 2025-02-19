package com.gel.driver.binary.codecs;

import com.gel.driver.binary.protocol.common.descriptors.CodecMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.UUID;

public abstract class CodecBase<T> implements Codec<T> {
    public final UUID id;
    public final @Nullable CodecMetadata metadata;
    private final Class<T> cls;

    public CodecBase(UUID id, @Nullable CodecMetadata metadata, Class<T> cls) {
        this.cls = cls;
        this.id = id;
        this.metadata = metadata;
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
    public Class<T> getConvertingClass() {
        return this.cls;
    }

    @Override
    public boolean canConvert(@NotNull Type type) {
        return type.equals(cls);
    }
}
