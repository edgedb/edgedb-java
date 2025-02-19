package com.gel.driver.binary.protocol;

import com.gel.driver.binary.codecs.Codec;
import io.netty.buffer.ByteBuf;

import java.util.List;

public class ExecuteResult {
    public final Codec<?> codec;
    public final List<ByteBuf> data;


    public ExecuteResult(Codec<?> codec, List<ByteBuf> data) {
        this.codec = codec;
        this.data = data;
    }
}
