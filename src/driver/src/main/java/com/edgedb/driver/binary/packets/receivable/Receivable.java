package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.packets.ServerMessageType;

public interface Receivable extends AutoCloseable {
    ServerMessageType getMessageType();

    @SuppressWarnings("unchecked")
    default <T extends Receivable> T as(Class<T> cls) {
        return (T)this;
    }

    default <T extends AutoCloseable> void release(T[] closeable) throws Exception {
        for(int i = 0; i != closeable.length; i++) {
            closeable[i].close();
        }
    }

    @Override
    default void close() throws Exception {}
}
