package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.codecs.CodecBase;

import java.util.UUID;

public abstract class ScalarCodecBase<T> extends CodecBase<T> implements ScalarCodec<T> {
    public ScalarCodecBase(UUID id, Class<T> cls) {
        super(id, cls);
    }
}
