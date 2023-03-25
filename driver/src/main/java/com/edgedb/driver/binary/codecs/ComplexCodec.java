package com.edgedb.driver.binary.codecs;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public interface ComplexCodec<T> extends Codec<T> {
    Collection<? extends Codec<?>> getRuntimeCodecs();
    void buildRuntimeCodecs() throws InvocationTargetException, InstantiationException, IllegalAccessException;
    Codec<?> getCodecFor(Class<?> type);
}