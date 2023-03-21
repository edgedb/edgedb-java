package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.codecs.CodecBase;

public abstract class ScalarCodecBase<T> extends CodecBase<T> implements ScalarCodec<T> {
    public ScalarCodecBase(Class<T> cls) {
        super(cls);
    }
}
