package com.gel.driver.binary.codecs.complex;


import java.util.function.Function;

public final  class ComplexCodecConverter<TSource, TTarget> {
    public final Class<TTarget> targetType;
    public final Function<TSource, TTarget> to;
    public final Function<TTarget, TSource> from;

    public ComplexCodecConverter(Class<TTarget> cls, Function<TSource, TTarget> to, Function<TTarget, TSource> from) {
        this.targetType = cls;
        this.to = to;
        this.from = from;
    }
}
