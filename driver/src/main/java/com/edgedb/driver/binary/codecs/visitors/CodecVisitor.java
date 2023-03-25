package com.edgedb.driver.binary.codecs.visitors;

import com.edgedb.driver.binary.codecs.Codec;

@FunctionalInterface
public interface CodecVisitor {
    Codec<?> visit(Codec<?> codec);
}
