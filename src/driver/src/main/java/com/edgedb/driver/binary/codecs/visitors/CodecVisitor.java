package com.edgedb.driver.binary.codecs.visitors;

import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.exceptions.EdgeDBException;

@FunctionalInterface
public interface CodecVisitor {
    Codec<?> visit(Codec<?> codec) throws EdgeDBException;
}
