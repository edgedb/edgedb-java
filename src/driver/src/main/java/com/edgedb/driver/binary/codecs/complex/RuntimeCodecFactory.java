package com.edgedb.driver.binary.codecs.complex;

import com.edgedb.driver.binary.codecs.RuntimeCodec;

@FunctionalInterface
public interface RuntimeCodecFactory {
    RuntimeCodec<?> create(Class<?> cls, ComplexCodecBase<?> parent, ComplexCodecConverter<?, ?> converter);
}
