package com.gel.driver.async;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

public final class AsyncEvent<T> {
    private final @NotNull List<Function<T, CompletionStage<?>>> listeners;

    private final @NotNull Semaphore semaphore;

    public AsyncEvent() {
        semaphore = new Semaphore(1);
        this.listeners = new ArrayList<>();
    }

    public void add(Function<T, CompletionStage<?>> handler) {
        semaphore.acquireUninterruptibly();

        try {
            listeners.add(handler);
        }
        finally {
            semaphore.release();
        }

    }

    public CompletionStage<Void> dispatch(T value) {
        if(listeners.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        semaphore.acquireUninterruptibly();

        try {
            var future = listeners.get(0).apply(value);

            for (int i = 1; i < listeners.size(); i++) {
                final var f = listeners.get(i).apply(value);
                future = future.thenCompose(v -> f);
            }

            return future.thenCompose(v -> CompletableFuture.completedFuture(null));
        }
        finally {
            semaphore.release();
        }
    }
}
