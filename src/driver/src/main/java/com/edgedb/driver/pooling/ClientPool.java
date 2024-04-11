package com.edgedb.driver.pooling;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class ClientPool implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ClientPool.class);

    public NioEventLoopGroup nettyEventGroup;
    public EventExecutorGroup duplexerGroup;

    private long size;
    private final @NotNull ConcurrentLinkedQueue<CompletableFuture<Void>> queue;

    private final @NotNull AtomicLong count;

    private final @NotNull AtomicInteger shareholders;

    public ClientPool(int initialSize) {
        this.size = initialSize;
        this.queue = new ConcurrentLinkedQueue<>();
        this.count = new AtomicLong(size);
        this.shareholders = new AtomicInteger(0);
    }

    public int getShareholders() {
        return this.shareholders.get();
    }

    public void addShareholder() {
        logger.debug("Shareholder added, now at {}", this.shareholders.incrementAndGet());
    }

    public boolean removeShareholder() {
        var c = this.shareholders.decrementAndGet();
        logger.debug("Shareholder removed, now at {}", c);
        return c <= 0;
    }

    public synchronized NioEventLoopGroup getNettyEventGroup() {
        if(this.nettyEventGroup == null) {
            this.nettyEventGroup = new NioEventLoopGroup();
        }

        return this.nettyEventGroup;
    }

    public synchronized EventExecutorGroup getDuplexerGroup() {
        if(this.duplexerGroup == null) {
            this.duplexerGroup = new DefaultEventExecutorGroup(8);
        }

        return this.duplexerGroup;
    }

    public long remaining() {
        return count.get();
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
        return new PoolContract(this, this::completeContract);
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

    @Override
    public void close() throws Exception {
        logger.debug("Closing client pool");

        if(this.nettyEventGroup != null) {
            this.nettyEventGroup.shutdownGracefully().addListener(f -> logger.debug("Client thread pool shutdown: {}", f.isSuccess()));
        }

        if(this.duplexerGroup != null) {
            this.duplexerGroup.shutdownGracefully().addListener(f -> logger.debug("Client duplex pool shutdown: {}", f.isSuccess()));
        }
    }
}
