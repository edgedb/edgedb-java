package com.edgedb.driver.binary.codecs;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.UUID;

public abstract class CodecBase<T> implements Codec<T> {
    public final UUID id;
    private final Class<T> cls;

    public CodecBase(UUID id, Class<T> cls) {
        this.cls = cls;
        this.id = id;
    }

    @Override
    public UUID getId() {
        return this.id;
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
