package com.gel.driver.binary.codecs;

public interface RuntimeCodec<T> extends Codec<T> {
    ComplexCodec<?> getBroker();
}
