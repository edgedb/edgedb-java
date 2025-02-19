package com.gel.driver.binary.codecs.visitors;

import com.gel.driver.binary.codecs.Codec;
import com.gel.driver.exceptions.GelException;

@FunctionalInterface
public interface CodecVisitor {
    Codec<?> visit(Codec<?> codec) throws GelException;
}
