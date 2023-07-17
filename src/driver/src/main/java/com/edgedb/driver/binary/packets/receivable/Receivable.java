package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.packets.ServerMessageType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Receivable extends AutoCloseable {
    Logger logger = LoggerFactory.getLogger(Receivable.class);

    ServerMessageType getMessageType();

    @SuppressWarnings("unchecked")
    default <T extends Receivable> @NotNull T as(Class<T> cls) {
        return (T)this;
    }

    default <T extends AutoCloseable> void release(T @NotNull [] closeable) throws Exception {
        for(int i = 0; i != closeable.length; i++) {
            closeable[i].close();
        }
    }

    @Override
    default void close() throws Exception {
        logger.debug("Closed {}:{}", this.hashCode(), getMessageType());
    }
}
