package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.packets.ServerMessageType;

public interface Receivable {
    ServerMessageType getMessageType();

    @SuppressWarnings("unchecked")
    default <T extends Receivable> T as(Class<T> cls) {
        return (T)this;
    }
}
