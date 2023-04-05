package com.edgedb.binary.codecs.complex;

import com.edgedb.binary.codecs.RuntimeCodec;

@FunctionalInterface
public interface RuntimeCodecFactory {
    RuntimeCodec<?> create(Class<?> cls, ComplexCodecBase<?> parent, ComplexCodecConverter<?, ?> converter);
}
