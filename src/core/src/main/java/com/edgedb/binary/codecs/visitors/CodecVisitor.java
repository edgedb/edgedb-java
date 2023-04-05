package com.edgedb.binary.codecs.visitors;

import com.edgedb.binary.codecs.Codec;
import com.edgedb.exceptions.EdgeDBException;

@FunctionalInterface
public interface CodecVisitor {
    Codec<?> visit(Codec<?> codec) throws EdgeDBException;
}
