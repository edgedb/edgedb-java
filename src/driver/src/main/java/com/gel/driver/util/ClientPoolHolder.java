package com.gel.driver.util;

import com.gel.driver.clients.BaseGelClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class ClientPoolHolder {
    private static final Logger logger = LoggerFactory.getLogger(ClientPoolHolder.class);

    private long size;
    private final @NotNull ConcurrentLinkedQueue<CompletableFuture<Void>> queue;

    private final @NotNull AtomicLong count;

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

    private @NotNull PoolContract lendContract() {
        return new PoolContract(this::completeContract);
    }

    private void completeContract(PoolContract contract) {
        logger.debug("Completing contract {}...", contract);

        if(queue.isEmpty()) {
            logger.debug("Empty contract queue, incrementing count");
            count.incrementAndGet();
            return;
        }


        logger.debug("Polling queue and completing...");
        Objects.requireNonNull(queue.poll()).complete(null);
    }

    public static class PoolContract implements AutoCloseable {
        private final Consumer<PoolContract> completer;
        private @Nullable BaseGelClient client;
        private @Nullable Consumer<BaseGelClient> onComplete;

        private PoolContract(Consumer<PoolContract> completer) {
            this.completer = completer;
        }

        public void register(BaseGelClient client, Consumer<BaseGelClient> onComplete) {
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
    }
}
