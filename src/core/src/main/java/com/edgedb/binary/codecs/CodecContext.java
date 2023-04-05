package com.edgedb.binary.codecs;

import com.edgedb.binary.codecs.visitors.TypeVisitor;
import com.edgedb.clients.EdgeDBBinaryClient;

public final class CodecContext {
    public final EdgeDBBinaryClient client;

    public CodecContext(EdgeDBBinaryClient client) {
        this.client = client;
    }

    public TypeVisitor getTypeVisitor() {
        return new TypeVisitor(this.client);
    }
}
