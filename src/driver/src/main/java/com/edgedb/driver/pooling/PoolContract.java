package com.edgedb.driver.pooling;

import com.edgedb.driver.clients.BaseEdgeDBClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class PoolContract implements AutoCloseable {
    private final @NotNull ClientPool pool;
    private final Consumer<PoolContract> completer;
    private @Nullable BaseEdgeDBClient client;
    private @Nullable Consumer<BaseEdgeDBClient> onComplete;

    public PoolContract(@NotNull ClientPool pool, Consumer<PoolContract> completer) {
        this.pool = pool;
        this.completer = completer;
    }

    public void register(BaseEdgeDBClient client, Consumer<BaseEdgeDBClient> onComplete) {
        this.client = client;
        this.onComplete = onComplete;
    }

    @Override
    public void close() {
        this.completer.accept(this);

        if(client != null && onComplete != null) {
            onComplete.accept(client);
        }
    }

    @Override
    public String toString() {
        return String.format("Contract(client := %s|onComplete := %s)", client, onComplete);
    }

    public @NotNull ClientPool getPool() {
        return pool;
    }
}
