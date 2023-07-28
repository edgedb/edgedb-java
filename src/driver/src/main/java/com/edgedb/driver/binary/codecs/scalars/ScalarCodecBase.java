package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.codecs.CodecBase;
import com.edgedb.driver.binary.protocol.common.descriptors.CodecMetadata;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class ScalarCodecBase<T> extends CodecBase<T> implements ScalarCodec<T> {
    public ScalarCodecBase(UUID id, @Nullable CodecMetadata metadata, Class<T> cls) {
        super(id, metadata, cls);
    }
}
