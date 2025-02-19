package com.gel.driver.binary.codecs;

import com.gel.driver.binary.codecs.visitors.TypeVisitor;
import com.gel.driver.clients.GelBinaryClient;
import org.jetbrains.annotations.NotNull;

public final class CodecContext {
    public final GelBinaryClient client;

    public CodecContext(GelBinaryClient client) {
        this.client = client;
    }

    public @NotNull TypeVisitor getTypeVisitor() {
        return new TypeVisitor(this.client);
    }
}
