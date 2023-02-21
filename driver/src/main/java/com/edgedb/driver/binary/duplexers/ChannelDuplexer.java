package com.edgedb.driver.binary.duplexers;

import com.edgedb.driver.async.ChannelCompletableFuture;
import com.edgedb.driver.binary.packets.receivable.Receivable;
import com.edgedb.driver.binary.packets.sendables.Sendable;
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
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class ChannelDuplexer extends Duplexer {
    private static final Logger logger = LoggerFactory.getLogger(ChannelDuplexer.class);

    private static final int PACKET_HEADER_SIZE = 5;

    // TODO: config these
    private static final int READ_TIMEOUT = 5000;
    private static final int WRITE_TIMEOUT = 5000;

    public final ChannelHandler channelHandler = new ChannelHandler();

    private final Queue<Receivable> messageQueue;
    private final Queue<CompletableFuture<Receivable>> readPromises;

    private final Object messageEnqueueReference = new Object();

    private final EdgeDBBinaryClient client;
    private final AtomicBoolean isDisconnected;

    private Channel channel;

    public class ChannelHandler extends ChannelInboundHandlerAdapter {
        public final CompletableFuture<Void> channelActivePromise;

        public ChannelHandler() {
            channelActivePromise = new CompletableFuture<>();
        }

        @Override
        public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            channelActivePromise.complete(null);
        }

        @Override
        public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
            super.channelRead(ctx, msg);

            synchronized (messageEnqueueReference) {
                messageQueue.add((Receivable)msg);

                for (var promise : readPromises) {
                    promise.complete((Receivable) msg);
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            //super.exceptionCaught(ctx, cause);
            logger.error("Channel failed", cause);
        }
    }

    public ChannelDuplexer(EdgeDBBinaryClient client) {
        this.client = client;
        this.isDisconnected = new AtomicBoolean(false);
        this.messageQueue = new ArrayDeque<>();
        this.readPromises = new ArrayDeque<>();
    }

    @Override
    public CompletionStage<Receivable> readNextAsync() {
        synchronized (messageEnqueueReference) {
            if(this.messageQueue.isEmpty()) {
                var promise = new CompletableFuture<Receivable>();
                readPromises.add(promise);
                return promise;
            } else {
                return CompletableFuture.completedFuture(this.messageQueue.poll());
            }
        }
    }

    @Override
    public CompletionStage<Void> sendAsync(Sendable packet, @Nullable Sendable... packets) throws SSLException {
        logger.debug("Starting to send packets to {}, is connected? {}", channel, !isDisconnected.get());

        // return attachment to ready promise to "queue" to send if this client hasn't connected.
        return this.channelHandler.channelActivePromise.thenCompose((v) -> {
            if(channel == null || isDisconnected.get()) {
                logger.debug("Reconnecting...");
                return client.reconnectAsync().thenCompose((w) -> {
                    try {
                        logger.debug("Sending after reconnect");
                        return this.sendAsync(packet, packets);
                    } catch (SSLException e) {
                        throw new CompletionException(e);
                    }
                }); // TODO: check for recursive loop
            }

            var result = ChannelCompletableFuture.completeFrom(channel.write(packet));

            if(packets != null) {
                for (var p : packets) {
                    result.thenCompose(channel.write(p));
                }
            }

            result.thenAccept((w) -> channel.flush());

            return result;
        });
    }

    @Override
    public CompletionStage<Void> duplexAsync(Function<DuplexResult, CompletionStage<Void>> func, @NotNull Sendable packet, @Nullable Sendable... packets) throws SSLException {
        final var duplexPromise = new CompletableFuture<Void>();

        return this.sendAsync(packet, packets)
                .thenCompose((v) -> processDuplexStep(func, duplexPromise))
                .thenCompose((v) -> duplexPromise);
    }

    private CompletionStage<Void> processDuplexStep(Function<DuplexResult, CompletionStage<Void>> func, CompletableFuture<Void> promise) {
        return readNextAsync()
                .thenCompose((packet) -> func.apply(new DuplexResult(packet, promise)))
                .thenCompose((v) -> {
                    if(promise.isDone())
                        return CompletableFuture.completedFuture(null);

                    return processDuplexStep(func, promise);
                });
    }

    public void init(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void reset() {

    }

    @Override
    public CompletableFuture<Void> disconnectAsync() {
        return null;
    }
}
