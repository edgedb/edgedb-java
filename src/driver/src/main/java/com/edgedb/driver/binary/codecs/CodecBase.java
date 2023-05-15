package com.edgedb.driver.binary.codecs;

import java.lang.reflect.Type;

public abstract class CodecBase<T> implements Codec<T> {
    private final Class<T> cls;

    public CodecBase(Class<T> cls) {
        this.cls = cls;
    }

    @Override
    public Class<T> getConvertingClass() {
        return this.cls;
    }

    @Override
    public boolean canConvert(Type type) {
        return type.equals(cls);
    }
}
