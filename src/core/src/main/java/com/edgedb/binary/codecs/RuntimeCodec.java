package com.edgedb.binary.codecs;

public interface RuntimeCodec<T> extends Codec<T> {
    ComplexCodec<?> getBroker();
}
