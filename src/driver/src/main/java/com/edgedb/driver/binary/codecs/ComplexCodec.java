package com.edgedb.driver.binary.codecs;

import java.util.Collection;

public interface ComplexCodec<T> extends Codec<T> {
    Collection<? extends Codec<?>> getRuntimeCodecs();
    void buildRuntimeCodecs();
    Codec<?> getCodecFor(Class<?> type);
}