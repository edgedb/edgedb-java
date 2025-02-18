package com.edgedb.driver.binary.duplexers;

import com.edgedb.driver.binary.PacketSerializer;
import com.edgedb.driver.binary.protocol.ProtocolProvider;
import com.edgedb.driver.binary.protocol.Receivable;
import com.edgedb.driver.binary.protocol.Sendable;
import com.edgedb.driver.clients.GelHttpClient;
import com.edgedb.driver.exceptions.ConnectionFailedException;
import com.edgedb.driver.exceptions.GelException;
import io.netty.buffer.ByteBufInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.*;

import static com.edgedb.driver.util.ComposableUtil.composeWith;

public class HttpDuplexer extends Duplexer {
    private static final Logger logger = LoggerFactory.getLogger(HttpDuplexer.class);
    private static final String HTTP_BINARY_CONTENT_TYPE = "application/x.edgedb.v_1_0.binary";

    private final GelHttpClient client;
    private final Semaphore lock;
    private final Executor lockExecutor;
    private final Queue<@NotNull Receivable> packetQueue;
    private final Queue<CompletableFuture<Receivable>> readPromises;
    private final HttpResponse.BodyHandler<List<Receivable>> bodyHandler;

    public HttpDuplexer(GelHttpClient client) {
        bodyHandler = PacketSerializer.createHandler(client);
        this.client = client;
        this.lock = new Semaphore(1);
        this.lockExecutor = Executors.newSingleThreadExecutor();
        this.packetQueue = new ArrayDeque<>();
        this.readPromises = new ArrayDeque<>();
    }

    @Override
    public ProtocolProvider getProtocolProvider() {
        return client.getProtocolProvider();
    }

    @Override
    public void reset() {
        packetQueue.clear();
        readPromises.clear();

        client.clearToken();
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
        return acquireLock("READ")
                .thenCompose((v) -> readNext0())
                .whenCompleteAsync((v,e) -> {
                    logger.debug("[READ]: Releasing lock");
                    lock.release();
                }, lockExecutor);
    }

    private CompletionStage<Receivable> readNext0() {
        logger.debug("Preforming read, is authed?: {}", isConnected());
        if(!isConnected()) {
            return CompletableFuture.failedFuture(
                    new GelException("Cannot perform read without authorization")
            );
        }

        logger.debug("Packet queue empty?: {}", packetQueue.isEmpty());
        if(packetQueue.isEmpty()) {
            var promise = new CompletableFuture<Receivable>();
            logger.debug("Enqueueing read promise {}...", promise.hashCode());
            readPromises.offer(promise);
            promise.whenComplete((v,e) -> {
                logger.debug("Read promise {} complete", promise.hashCode());
            });
            return promise;
        } else {
            logger.debug("Completing from polled packet");
            return CompletableFuture.completedFuture(packetQueue.poll());
        }
    }

    @Override
    public CompletionStage<Void> send(Sendable packet, @Nullable Sendable... packets) {
        return acquireLock("WRITE")
                .thenCompose((v) -> send0(packet, packets))
                .whenCompleteAsync((v,e) -> {
                    logger.debug("[WRITE]: Releasing lock");
                    lock.release();
                }, lockExecutor);
    }

    private CompletionStage<Void> send0(Sendable packet, @Nullable Sendable... packets) {
        return verifyAuthenticated()
                .thenApply((v) -> {
                    try {
                        logger.debug("Creating buffer stream");
                        return new ByteBufInputStream(PacketSerializer.serialize(packet, packets), true);
                    } catch (OperationNotSupportedException e) {
                        logger.debug("Failed to create buffer stream", e);
                        throw new CompletionException(e);
                    }
                })
                .thenApply(stream ->
                    HttpRequest.newBuilder()
                            .uri(client.getExecUri())
                            .header("Authorization", "Bearer " + client.getToken())
                            .header("Content-Type", HTTP_BINARY_CONTENT_TYPE)
                            .header("X-EdgeDB-User", client.getConnectionArguments().getUsername())
                            .POST(HttpRequest.BodyPublishers.ofInputStream(() -> stream))
                            .build()
                )
                .thenCompose((request) -> {
                    logger.debug("Sending execution request...");
                    return client.httpClient.sendAsync(request, bodyHandler);
                })
                .thenCompose(GelHttpClient::ensureSuccess)
                .thenAccept(response -> {
                    logger.debug("Enqueueing {} packets", response.body().size());
                    for(var receivable : response.body()) {
                        packetQueue.offer(receivable);
                    }
                })
                .thenCompose((v) -> processReadPromises());
    }

    private CompletionStage<Void> processReadPromises() {
        logger.debug(
                "Processing read promises. has promises?: {}, has data?: {}",
                !readPromises.isEmpty(), !packetQueue.isEmpty()
        );

        if(!readPromises.isEmpty() && !packetQueue.isEmpty()) {
            var promise = readPromises.poll();
            var receivable = Objects.requireNonNull(packetQueue.poll()); // closed by the 'composeWith' func

            logger.debug("Executing promise {} with {}", promise.hashCode(), receivable.getMessageType());

            return composeWith(receivable, (v) -> promise.completeAsync(() -> v))
                    .thenCompose((v) -> processReadPromises());
        }

        logger.debug("Completed read promise steps");
        return CompletableFuture.completedFuture(null);
    }

    private CompletionStage<Void> verifyAuthenticated() {
        return CompletableFuture
                .runAsync(() -> {
                    logger.debug("Verifying authentication state... is authed?: {}", isConnected());
                    if(!isConnected()) {
                        throw new CompletionException(
                                new ConnectionFailedException("Cannot send to an unauthorized connection")
                        );
                    }
                });
    }

    private CompletionStage<Void> acquireLock(String operation) {
        return CompletableFuture
                .runAsync(() -> {
                    try {
                        logger.debug("[{}]: Acquiring lock...", operation);
                        if(!lock.tryAcquire(
                                client.getConfig().getMessageTimeoutValue(),
                                client.getConfig().getMessageTimeoutUnit())
                        ) {
                            logger.debug("[{}]: Lock timed out", operation);
                            throw new CompletionException(
                                    new TimeoutException("A message read process passed the configured message timeout")
                            );
                        }

                        logger.debug("[{}]: Lock acquired", operation);
                    } catch (InterruptedException v) {
                        throw new CompletionException(v);
                    }
                }, lockExecutor);
    }


    @Override
    public CompletionStage<Void> duplex(DuplexCallback func, @NotNull Sendable packet, @Nullable Sendable... packets) {
        return acquireLock("DUPLEX")
                .thenCompose((v) -> duplex0(func, packet, packets))
                .whenCompleteAsync((v,e) -> {
                    logger.debug("[DUPLEX]: Releasing lock");
                    lock.release();
                }, lockExecutor);
    }

    private CompletionStage<Void> duplex0(DuplexCallback func, @NotNull Sendable packet, @Nullable Sendable... packets) {
        var duplexPromise = new CompletableFuture<Void>();
        return send0(packet, packets)
                .thenCompose((v) -> processDuplexStep(func, duplexPromise));
    }

    private CompletionStage<Void> processDuplexStep(DuplexCallback func, CompletableFuture<Void> promise) {
        return readNext0()
                .thenApply((packet) -> new DuplexResult(packet, promise))
                .thenCompose((state) -> {
                    try {
                        return func.process(state);
                    } catch (GelException | OperationNotSupportedException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                })
                .thenCompose((v) -> {
                    if(promise.isDone()) {
                        if(promise.isCompletedExceptionally() || promise.isCancelled()) {
                            return promise;
                        }

                        return CompletableFuture.completedFuture(null);
                    }

                    return processDuplexStep(func, promise);
                });
    }
}
