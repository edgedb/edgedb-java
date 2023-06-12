package com.edgedb.driver.binary.duplexers;

import com.edgedb.driver.async.ChannelCompletableFuture;
import com.edgedb.driver.binary.packets.receivable.Receivable;
import com.edgedb.driver.binary.packets.sendables.Sendable;
import com.edgedb.driver.binary.packets.sendables.Terminate;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static com.edgedb.driver.util.ComposableUtil.composeWith;

public class ChannelDuplexer extends Duplexer {
    private static final Logger logger = LoggerFactory.getLogger(ChannelDuplexer.class);

    public final ChannelHandler channelHandler = new ChannelHandler();

    private final Queue<Receivable> messageQueue;
    private final Queue<CompletableFuture<Receivable>> readPromises;

    private final ReentrantLock messageEnqueueLock = new ReentrantLock();

    private final EdgeDBBinaryClient client;

    private boolean isConnected;

    private @Nullable Channel channel;


    @io.netty.channel.ChannelHandler.Sharable
    public class ChannelHandler extends ChannelInboundHandlerAdapter {
        private CompletableFuture<Void> channelActivePromise;

        public ChannelHandler() {
            channelActivePromise = new CompletableFuture<>();
        }

        @Override
        public void channelActive(@NotNull ChannelHandlerContext ctx) {
            isConnected = true;
            channelActivePromise.complete(null);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if(evt.equals("RESET")) {
                channelActivePromise = new CompletableFuture<>();
            } else if (evt.equals("TIMEOUT")) {
                var exc = new TimeoutException("A message read process passed the configured message timeout");
                for(var promise : readPromises) {
                    promise.completeExceptionally(exc);
                }
            }
        }

        @Override
        public void channelInactive(@NotNull ChannelHandlerContext ctx) {
            isConnected = false;
        }

        @Override
        public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) {
            var protocolMessage = (Receivable)msg;
            logger.debug("Read fired, entering message lock, message type {}", protocolMessage.getMessageType());

            int completeCount = 0;


            try {
                if(!messageEnqueueLock.tryLock(client.getConfig().getMessageTimeoutValue(), client.getConfig().getMessageTimeoutUnit())) {
                    ctx.fireUserEventTriggered("TIMEOUT");
                    return;
                }
            } catch (InterruptedException e) {
                ctx.fireExceptionCaught(e);
                return;
            }

            try {
                logger.debug("Dependant promises empty?: {}", readPromises.isEmpty());

                if(readPromises.isEmpty()) {
                    logger.debug("Enqueuing message into message queue");
                    messageQueue.add(protocolMessage);
                }
                else {
                    logger.debug("Completing {} message promise(s)", readPromises.size());

                    // we don't want to iterate and complete within the lock, since the complete method *can* enqueue
                    // more promises.
                    completeCount = readPromises.size();
                }
            } finally {
                messageEnqueueLock.unlock();
            }

            for(int i = 0; i != completeCount; i++) {
                var promise = readPromises.poll();

                if(promise == null) {
                    break;
                }

                logger.debug(
                        "Completing promise {} with message type {}; already complete?: {}",
                        promise.hashCode(), protocolMessage.getMessageType(), promise.isDone()
                );

                promise.complete(protocolMessage);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Channel failed", cause);
        }

        public synchronized CompletableFuture<Void> whenReady() {
            if(this.channelActivePromise.isDone()) {
                return CompletableFuture.completedFuture(null);
            } else {
                return this.channelActivePromise;
            }
        }
    }

    public ChannelDuplexer(EdgeDBBinaryClient client) {
        this.client = client;
        this.messageQueue = new ArrayDeque<>();
        this.readPromises = new ArrayDeque<>();
    }

    @Override
    public CompletionStage<Receivable> readNext() {
        logger.debug("Entering message queue lock");

        try {
            if(!messageEnqueueLock.tryLock(client.getConfig().getMessageTimeoutValue(), client.getConfig().getMessageTimeoutUnit())) {
                return CompletableFuture.failedFuture(new TimeoutException("A message processor passed the configured message timeout"));
            }
        } catch (InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }

        try {
            logger.debug("Queue empty?: {}", this.messageQueue.isEmpty());

            if(this.messageQueue.isEmpty()) {
                logger.debug("Creating message wait promise");

                var promise = new CompletableFuture<Receivable>()
                        .orTimeout(
                                client.getConfig().getMessageTimeoutValue(),
                                client.getConfig().getMessageTimeoutUnit()
                        );

                final var readPromiseId = promise.hashCode();

                promise.whenComplete((v,e) -> {
                    logger.debug("Read promise completed, ID: {}, is success?: {}", promise.hashCode(), e == null && !promise.isCancelled());
                });

                logger.debug("Enqueueing read promise: ID: {}", readPromiseId);
                readPromises.add(promise);
                return promise;
            } else {
                var message = this.messageQueue.poll();
                logger.debug("Returning polled message {}", message.getMessageType());
                return CompletableFuture.completedFuture(message);
            }
        }
        finally {
            messageEnqueueLock.unlock();
        }
    }

    @Override
    public CompletionStage<Void> send(Sendable packet, @Nullable Sendable... packets){
        logger.debug("Starting to send packets to {}, is connected? {}", channel, isConnected);

        // return attachment to ready promise to "queue" to send if this client hasn't connected.
        return this.channelHandler.whenReady().thenCompose((v) -> {
            if(channel == null || !isConnected) {
                logger.debug("Reconnecting...");
                return client.reconnect().thenCompose((w) -> {
                    logger.debug("Sending after reconnect");
                    return this.send(packet, packets);
                }); // TODO: check for recursive loop
            }

            logger.debug("Beginning packet encoding and writing...");
            var result = ChannelCompletableFuture.completeFrom(channel.write(packet));

            if(packets != null) {
                for (var p : packets) {
                    result.thenCompose(channel.write(p));
                }
            }

            logger.debug("Flushing data...");
            channel.flush();
            logger.debug("Flush complete, returning write proxy task");
            return result;
        });
    }

    @Override
    public CompletionStage<Void> duplex(Function<DuplexResult, CompletionStage<Void>> func, @NotNull Sendable packet, @Nullable Sendable... packets) throws SSLException {
        var firstHashcode = packet.hashCode();
        final var duplexId = 31 * firstHashcode + Arrays.hashCode(packets);
        logger.debug("Starting duplex step, ID: {}", duplexId);
        final var duplexPromise = new CompletableFuture<Void>();

        duplexPromise.whenComplete((v,e) -> {
            logger.debug("Duplex step complete, ID: {}, isCancelled?: {}, isExceptional?: {}", duplexId, duplexPromise.isCancelled(), e != null);
        });

        return this.send(packet, packets)
                .thenCompose((v) -> processDuplexStep(func, duplexPromise, duplexId))
                .thenCompose((v) -> duplexPromise);
    }

    private CompletionStage<Void> processDuplexStep(Function<DuplexResult, CompletionStage<Void>> func, CompletableFuture<Void> promise, int id) {
        logger.debug("Handling duplex step, ID: {}", id);

        return composeWith(readNext(), (packet) -> {
            logger.debug("Invoking duplex consumer, ID: {}, Message: {}", id, packet.getMessageType());
            return func.apply(new DuplexResult(packet, promise));
        }).thenCompose(v -> {
            logger.debug(
                    "Post-invoke duplex step ID: {}, isDone?: {}, isCancelled?: {}, isExceptional?: {}, ",
                    id, promise.isDone(), promise.isCancelled(), promise.isCompletedExceptionally()
            );

            if(promise.isDone()) {
                if(promise.isCompletedExceptionally() || promise.isCancelled()) {
                    logger.debug("Returning failed-state promise to callee, ID: {}", id);
                    return promise;
                }

                logger.debug("Returning completed state promise, ID: {}", id);
                return CompletableFuture.completedFuture(null);
            }

            logger.debug("Continuing duplex step for ID: {}", id);
            return processDuplexStep(func, promise, id);
        });
    }

    public void init(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void reset() {
        if(this.channel != null) {
            channel.pipeline().fireUserEventTriggered("RESET");
        }

    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public CompletionStage<Void> disconnect() {
        if(this.channel == null) {
            return CompletableFuture.completedFuture(null);
        }

        if(this.channel.isOpen()) {
            return send(new Terminate())
                    .thenCompose(v -> ChannelCompletableFuture.completeFrom(this.channel.disconnect()));
        }

        return ChannelCompletableFuture.completeFrom(this.channel.disconnect());
    }
}
