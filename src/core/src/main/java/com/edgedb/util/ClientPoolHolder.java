package com.edgedb.util;

import com.edgedb.clients.BaseEdgeDBClient;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class ClientPoolHolder {
    private long size;
    private final ConcurrentLinkedQueue<CompletableFuture<Void>> queue;

    private final AtomicLong count;

    public ClientPoolHolder(int initialSize) {
        this.size = initialSize;
        this.queue = new ConcurrentLinkedQueue<>();
        this.count = new AtomicLong(size);
    }

    public long remaining() {
        return this.size - count.get();
    }

    public void resize(long newValue) {
        if(newValue == this.size) {
            return;
        }

        count.getAndUpdate(v -> v + (newValue - size));
        this.size = newValue;
    }

    public CompletionStage<PoolContract> acquireContract() {
        var c = count.decrementAndGet();

        if(c < 0) {
            count.compareAndSet(-1, 0);

            var promise = new CompletableFuture<Void>();

            queue.add(promise);

            return promise.thenApply(v -> lendContract());
        }
        else {
            return CompletableFuture.completedFuture(lendContract());
        }
    }

    private PoolContract lendContract() {
        return new PoolContract(this::completeContract);
    }

    private void completeContract(PoolContract contract) {
        if(queue.isEmpty()) {
            count.incrementAndGet();
            return;
        }

        queue.poll().complete(null);
    }

    public static class PoolContract implements AutoCloseable {
        private final Consumer<PoolContract> completer;
        private @Nullable BaseEdgeDBClient client;
        private @Nullable Consumer<BaseEdgeDBClient> onComplete;

        private PoolContract(Consumer<PoolContract> completer) {
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
    }
}
