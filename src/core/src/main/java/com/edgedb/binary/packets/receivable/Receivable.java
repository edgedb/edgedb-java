package com.edgedb.binary.packets.receivable;

import com.edgedb.binary.packets.ServerMessageType;

public interface Receivable {
    ServerMessageType getMessageType();

    @SuppressWarnings("unchecked")
    default <T extends Receivable> T as(Class<T> cls) {
        return (T)this;
    }
}
