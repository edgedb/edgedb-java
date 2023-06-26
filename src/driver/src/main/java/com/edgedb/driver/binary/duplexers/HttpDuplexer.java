package com.edgedb.driver.binary.duplexers;

import com.edgedb.driver.binary.packets.receivable.Receivable;
import com.edgedb.driver.binary.packets.sendables.Sendable;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.clients.EdgeDBHttpClient;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;

public class HttpDuplexer extends Duplexer {
    private static final String HTTP_BINARY_CONTENT_TYPE = "application/x.edgedb.v_1_0.binary";

    private final EdgeDBHttpClient client;
    private final Semaphore lock;
    private final Executor lockExecutor;
    public HttpDuplexer(EdgeDBHttpClient client) {
        this.client = client;
        this.lock = new Semaphore(1);
        this.lockExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void reset() {

    }

    @Override
    public boolean isConnected() {
        return client.getToken() != null;
    }

    @Override
    public CompletionStage<Void> disconnect() {
        return CompletableFuture.runAsync(client::clearToken);
    }

    @Override
    public CompletionStage<Receivable> readNext() {
        return acquireLock()
                .thenCompose((v) -> readNext0())
                .whenCompleteAsync((v,e) -> lock.release(), lockExecutor);
    }

    private CompletionStage<Receivable> readNext0() {
        if(!isConnected()) {
            return CompletableFuture.failedFuture(
                    new EdgeDBException("Cannot preform read without authorization")
            );


        }
    }

    @Override
    public CompletionStage<Void> send(Sendable packet, @Nullable Sendable... packets) {
        return acquireLock()
                .thenCompose((v) -> send0(packet, packets));
    }

    private CompletionStage<Void> send0(Sendable packet, @Nullable Sendable... packets) {

    }

    private CompletionStage<Void> acquireLock() {
        return CompletableFuture
                .runAsync(() -> {
                    try {
                        if(!lock.tryAcquire(
                                client.getConfig().getMessageTimeoutValue(),
                                client.getConfig().getMessageTimeoutUnit())
                        ) {
                            throw new CompletionException(
                                    new TimeoutException("A message read process passed the configured message timeout")
                            );
                        }
                    } catch (InterruptedException v) {
                        throw new CompletionException(v);
                    }
                }, lockExecutor);
    }


    @Override
    public CompletionStage<Void> duplex(DuplexCallback func, @NotNull Sendable packet, @Nullable Sendable... packets) {
        return null;
    }
}
