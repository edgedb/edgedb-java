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
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static com.edgedb.driver.util.ComposableUtil.composeWith;

public class ChannelDuplexer extends Duplexer {
    private static final Logger logger = LoggerFactory.getLogger(ChannelDuplexer.class);

    public final ChannelHandler channelHandler = new ChannelHandler();

    private final Queue<Receivable> messageQueue;
    private final Queue<CompletableFuture<Receivable>> readPromises;

    private final Object messageEnqueueReference = new Object();

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
            }
        }

        @Override
        public void channelInactive(@NotNull ChannelHandlerContext ctx) {
            isConnected = false;
        }

        @Override
        public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) {
            synchronized (messageEnqueueReference) {
                if(readPromises.isEmpty()) {
                    messageQueue.add((Receivable)msg);
                }
                else {
                    for (var promise : readPromises) {
                        promise.complete((Receivable) msg);
                    }
                }
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
        synchronized (messageEnqueueReference) {
            if(this.messageQueue.isEmpty()) {
                var promise = new CompletableFuture<Receivable>()
                        .orTimeout(
                                client.getConfig().getMessageTimeoutValue(),
                                client.getConfig().getMessageTimeoutUnit()
                        );

                readPromises.add(promise);
                return promise;
            } else {
                return CompletableFuture.completedFuture(this.messageQueue.poll());
            }
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
        final var duplexPromise = new CompletableFuture<Void>();

        return this.send(packet, packets)
                .thenCompose((v) -> processDuplexStep(func, duplexPromise))
                .thenCompose((v) -> duplexPromise);
    }

    private CompletionStage<Void> processDuplexStep(Function<DuplexResult, CompletionStage<Void>> func, CompletableFuture<Void> promise) {
        return composeWith(readNext(), (packet) -> func.apply(new DuplexResult(packet, promise)))
                .thenCompose(v -> {
                    if(promise.isDone()) {
                        if(promise.isCompletedExceptionally() || promise.isCancelled()) {
                            return promise;
                        }

                        return CompletableFuture.completedFuture(null);
                    }


                    return processDuplexStep(func, promise);
                }
        );
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
