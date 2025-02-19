package com.gel.driver.binary.codecs.scalars;

import com.gel.driver.binary.codecs.CodecBase;
import com.gel.driver.binary.protocol.common.descriptors.CodecMetadata;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class ScalarCodecBase<T> extends CodecBase<T> implements ScalarCodec<T> {
    public ScalarCodecBase(UUID id, @Nullable CodecMetadata metadata, Class<T> cls) {
        super(id, metadata, cls);
    }
}
