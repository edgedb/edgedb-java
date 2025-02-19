package com.gel.driver.binary.protocol.common.descriptors;

import com.gel.driver.binary.codecs.Codec;
import com.gel.driver.binary.protocol.TypeDescriptorInfo;

public final class CodecAncestor {
    public final Codec<?> codec;
    public final TypeDescriptorInfo<?> descriptor;

    public CodecAncestor(Codec<?> codec, TypeDescriptorInfo<?> descriptor) {
        this.codec = codec;
        this.descriptor = descriptor;
    }
}
