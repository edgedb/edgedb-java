package com.edgedb.driver.binary.protocol.common.descriptors;

import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.protocol.TypeDescriptorInfo;

public final class CodecAncestor {
    public final Codec<?> codec;
    public final TypeDescriptorInfo<?> descriptor;

    public CodecAncestor(Codec<?> codec, TypeDescriptorInfo<?> descriptor) {
        this.codec = codec;
        this.descriptor = descriptor;
    }
}
