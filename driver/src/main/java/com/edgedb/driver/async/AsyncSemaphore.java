package com.edgedb.driver.async;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class AsyncSemaphore {

    private final AtomicInteger count;
    private final ConcurrentLinkedQueue<CompletableFuture<Void>> waitHandles;
    private final Integer permits;

    public AsyncSemaphore(int permits) {
        this.waitHandles = new ConcurrentLinkedQueue<>();
        this.permits = permits;
        this.count = new AtomicInteger(permits);
    }

    public CompletionStage<Void> aquire() {
        var state = count.updateAndGet(x -> x > 0 ? x - 1 : -1);

        if(state == -1) {
            count.compareAndSet(-1, 0);
            state = 0;
        }

        if(state == 0) {
            var promise = new CompletableFuture<Void>();
            synchronized (permits) {
                waitHandles.add(promise);
            }

            return promise;
        }
        else {
            return CompletableFuture.completedFuture(null);
        }
    }

    public boolean release() {
        var state = count.updateAndGet(x -> x == permits ? -1 : x + 1);

        if(state == -1) {
            count.compareAndSet(-1, permits);
            return false;
        }

        if(state <= permits){
            Objects.requireNonNull(waitHandles.poll()).complete(null);
        }

        return true;
    }
}
