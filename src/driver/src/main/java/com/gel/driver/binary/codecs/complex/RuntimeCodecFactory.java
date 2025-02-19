package com.gel.driver.binary.codecs.complex;

import com.gel.driver.binary.codecs.RuntimeCodec;

@FunctionalInterface
public interface RuntimeCodecFactory {
    RuntimeCodec<?> create(Class<?> cls, ComplexCodecBase<?> parent, ComplexCodecConverter<?, ?> converter);
}
