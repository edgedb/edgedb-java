package com.edgedb.driver.ssl;

import com.edgedb.driver.async.CompletableHandlerFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ReadPendingException;
import java.util.*;
import java.util.concurrent.*;

public class AsyncSSLChannel {
    // temp, will be config'd
    private static final int HANDSHAKE_TIMEOUT = 5000;

    private static final Logger logger = LoggerFactory.getLogger(AsyncSSLChannel.class);
    protected final SSLEngine engine;
    protected AsynchronousSocketChannel channel;

    private final ByteBuffer myNetData;
    private ByteBuffer peerNetData;

    // read buffers
    private final Queue<ByteBuffer> appDataQueue;
    private final Queue<ByteBuffer> allocCache;

    private final Executor sslPool;

    public AsyncSSLChannel(@NotNull SSLEngine engine, @Nullable Executor sslPool) {
        this.engine = engine;

        this.engine.setUseClientMode(true);

        this.sslPool = sslPool == null ? ForkJoinPool.commonPool() : sslPool;

        var session = this.engine.getSession();

        this.myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
        this.peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());


        this.appDataQueue = new ArrayDeque<>();
        this.allocCache = new ArrayDeque<>();
    }

    public void setALPNProtocols(String... protocols) {
        var params = this.engine.getSSLParameters();
        params.setApplicationProtocols(protocols);
        this.engine.setSSLParameters(params);
    }

    public CompletionStage<Void> connect(String host, int port) throws IOException {
        var result = new CompletableHandlerFuture<Void, Void>();

        channel = AsynchronousSocketChannel.open();
        channel.connect(new InetSocketAddress(host, port), null, result);

        this.engine.beginHandshake();

        return result
                .thenCompose((v) -> doHandshake());
    }

    public CompletionStage<Integer> readAsync(ByteBuffer buffer, long timeout, TimeUnit timeUnit) {
        logger.debug("Starting read...");


        if(this.tryReadFromQueue(buffer, this.appDataQueue)) {
            return CompletableFuture.completedFuture(buffer.position());
        }

        var context = new ChannelContext(this, ChannelOperation.READ, timeout, timeUnit);

        var preReadPosition = buffer.position();

        // perform actual read
        return processHandshakePart(SSLEngineResult.HandshakeStatus.NEED_UNWRAP, context)
                .thenCompose(this::runHandshakeLoop)
                .thenCompose((v) -> {
                    // copy out data to the external buffer
                    // if the target buffer is still not filled after we dump the buffers in
                    // context to it, re-run a read NOTE: timeout delta time isn't calculated here,
                    // do we need that?

                    // puts data into the buffer if possible, note even if we return false
                    // here the buffer *can* contain data we've read
                    if (!tryReadFromQueue(buffer, context.serverAppDataBuffers)) {
                        return readAsync(buffer, timeout, timeUnit);
                    }

                    return CompletableFuture.completedFuture(buffer.position() - preReadPosition);
                });
    }

    public CompletionStage<Void> write(ByteBuffer buffer, long timeout, TimeUnit timeUnit) {
        var context = new ChannelContext(this, buffer, ChannelOperation.WRITE, timeout, timeUnit);

        return processHandshakePart(SSLEngineResult.HandshakeStatus.NEED_WRAP, context)
                .thenCompose(this::runHandshakeLoop);
    }

    public CompletionStage<Void> disconnect() {
        return CompletableFuture.completedFuture(null);
    }

    private CompletionStage<Void> doHandshake() {
        logger.debug("starting handshake");

        this.peerNetData.clear();
        this.myNetData.clear();

        var context = new ChannelContext(this, ChannelOperation.HANDSHAKE, HANDSHAKE_TIMEOUT, TimeUnit.MILLISECONDS);

        return this.processHandshakePart(this.engine.getHandshakeStatus(), context)
                .thenCompose(this::runHandshakeLoop)
                .thenAccept((v) -> {
                    // restore data buffers (if any)
                    for(var buffer : context.serverAppDataBuffers) {
                        if(buffer.position() == 0) { // empty
                            this.cacheBuffer(buffer);
                        } else {
                            // buffer has data, add to app data queue
                            this.appDataQueue.add(buffer);
                        }
                    }
                });
    }

    private CompletionStage<Void> runHandshakeLoop(HandshakeProcessResult result) {
        if(result.hasErrored) {
            return CompletableFuture.failedFuture(new SSLException(result.errorMessage));
        }

        // examine the process status
        if(result.processStatus != null) {
            return examine(result);
        }

        if(result.handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED || result.handshakeStatus == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            logger.debug("Handshake finished with status {}", result.handshakeStatus);
            return CompletableFuture.completedFuture(null);
        }

        return processHandshakePart(result.handshakeStatus, result.context).thenCompose(this::runHandshakeLoop);
    }

    private CompletionStage<Void> examine(
            HandshakeProcessResult result
    ) {
        if(result.processStatus != null) {
            logger.debug("examining status {} on {}", result.processStatus, result.operation);

            switch (result.processStatus) {
                case OK:
                    if(result.operation == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                        if(result.context.getState() == ChannelState.CONTINUE_UNWRAP) {
                            logger.debug("Skipping flush as more data needs to be wrapped");
                            break;
                        }

                        logger.debug("Flushing {} net bytes", this.myNetData.position());
                        this.myNetData.flip();
                        return this.flushMyNetData(result.context)
                                .thenCompose((v) -> processHandshakePart(result.handshakeStatus, result.context))
                                .thenCompose(this::runHandshakeLoop);
                    }
                    break;
                case BUFFER_OVERFLOW:
                    switch (result.operation) {
                        case NEED_UNWRAP:
                        {
                            // should be handled by the unwrap logic.
                            logger.debug("Illegal access to BUFFER_OVERFLOW on NEED_UNWRAP");
                            return CompletableFuture.failedFuture(new IllegalStateException("Illegal access to BUFFER_OVERFLOW on NEED_UNWRAP"));
                        }
                        case NEED_WRAP:
                        {
                            logger.debug("Scaling up peers net data buffer");
                            var sz = engine.getSession().getPacketBufferSize();
                            this.peerNetData = sz > this.peerNetData.capacity()
                                    ? ByteBuffer.allocate(sz)
                                    : ByteBuffer.allocate(this.peerNetData.capacity() * 2);
                        }
                        break;
                    }
                    break;
                case BUFFER_UNDERFLOW:
                    logger.debug("Scaling up peers net data");
                    if(result.operation != SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                        return CompletableFuture.failedFuture(new SSLException("Buffer underflow occurred after a wrap. I don't think we should ever get here."));
                    }

                    var sz = engine.getSession().getPacketBufferSize();
                    if(sz < this.peerNetData.limit()) {
                        break;
                    }

                    var buffer = sz > this.peerNetData.capacity()
                            ? ByteBuffer.allocate(sz)
                            : ByteBuffer.allocate(this.peerNetData.capacity() * 2);

                    this.peerNetData.flip();
                    buffer.put(this.peerNetData);
                    this.peerNetData = buffer;
                    break;
            }
        }

        return processHandshakePart(result.handshakeStatus, result.context).thenCompose(this::runHandshakeLoop);
    }

    private CompletionStage<Void> flushMyNetData(ChannelContext context) {
        if(this.myNetData.hasRemaining())
        {
            var result = new CompletableHandlerFuture<Integer, Void>();
            this.channel.write(this.myNetData, context.timeout, context.units,null, result);
            return result.thenCompose((v) -> flushMyNetData(context));
        }

        logger.debug("Flush complete, net data buffer empty");
        return CompletableFuture.completedFuture(null);
    }

    protected CompletionStage<HandshakeProcessResult> processHandshakePart(SSLEngineResult.HandshakeStatus handshakeStatus, ChannelContext context) {
        logger.debug("Starting to process part {}", handshakeStatus);

        switch (handshakeStatus) {
            case NEED_UNWRAP:

                if(context.getState() != ChannelState.CONTINUE_UNWRAP) {
                    var result = new CompletableHandlerFuture<Integer, Void>();

                    logger.debug("Beginning read for unwrap");
                    try {
                        this.channel.read(this.peerNetData, context.timeout, context.units, null, result);
                    }
                    catch (ReadPendingException r) {
                        logger.debug("Got read pending error", r);
                        throw new CompletionException(r);
                    }

                    return result.flatten().thenApply((v) -> {
                        logger.debug("Read complete");
                        return handleUnwrap(v, handshakeStatus, context);
                    });
                }
                else {
                    return CompletableFuture.supplyAsync(() -> handleUnwrap(this.peerNetData.remaining(), handshakeStatus, context));
                }
            case NEED_WRAP:
                this.myNetData.clear();

                try {
                    logger.debug("Starting to wrap net data...");

                    // TODO: just like wrap, we need to continue wrapping and handling engine tasks that need to be run

                    var engineResult = engine.wrap(context.clientAppData, this.myNetData);
                    var hsStatus = engineResult.getHandshakeStatus();
                    var status = engineResult.getStatus();

                    logger.debug("Wrapping result {}. wrapped {} bytes with {} remaining", status, engineResult.bytesConsumed(), context.clientAppData.remaining());

                    return CompletableFuture.completedFuture(HandshakeProcessResult.fromSuccess(
                            handshakeStatus,
                            hsStatus,
                            status,
                            context
                    ));

                } catch (SSLException e) {
                    logger.error("A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...", e);
                    engine.closeOutbound();
                    return CompletableFuture.completedFuture(HandshakeProcessResult.fromError(handshakeStatus, e.getMessage(), context));
                }
            case NEED_TASK:
                logger.debug("Delegating and running tasks");
                List<CompletableFuture<Void>> tasks = new ArrayList<>();
                Runnable task;
                while((task = engine.getDelegatedTask()) != null) {
                    tasks.add(CompletableFuture.runAsync(task, this.sslPool));
                }

                return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                        .thenApply((v) -> {
                            logger.debug("Tasks completed");
                            return HandshakeProcessResult.fromSuccess(
                                    handshakeStatus,
                                    this.engine.getHandshakeStatus(),
                                    null,
                                    context
                            );
                        });


            case FINISHED:
            case NOT_HANDSHAKING:
                logger.debug("Handshake complete");
                context.setState(ChannelState.COMPLETE);
                return CompletableFuture.completedFuture(HandshakeProcessResult.fromSuccess(
                        handshakeStatus,
                        handshakeStatus,
                        null,
                        context
                ));
            default:
                logger.debug("Got unknown state {}", handshakeStatus);
                return CompletableFuture.completedFuture(HandshakeProcessResult.fromError(
                        handshakeStatus,
                        "Unknown engine state " + handshakeStatus,
                        context
                ));
        }
    }

    private HandshakeProcessResult handleUnwrap(Integer count, SSLEngineResult.HandshakeStatus handshakeStatus, ChannelContext context)  {
        logger.debug("unwrapping {} remaining bytes", count);

        if(count < 0) {
            if(engine.isInboundDone() && engine.isOutboundDone()) {
                logger.debug("Engine state died");
                return HandshakeProcessResult.fromError(
                        handshakeStatus,
                        "Engine state is ruined, the universe has collapsed", // TODO: better err msg here
                        context
                );
            }

            try {
                engine.closeInbound();
            }
            catch (SSLException e) {
                logger.debug("Forced closure of engine");
                return HandshakeProcessResult.fromError(
                        handshakeStatus,
                        "This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream: " + e.getMessage(),
                        context);
            }

            engine.closeOutbound();

            return HandshakeProcessResult.fromSuccess(handshakeStatus, engine.getHandshakeStatus(), null, context);
        }

        peerNetData.flip();

        try {
            logger.debug("Unwrapping data...");

            var buffer = getDataBuffer();

            var engineResult = engine.unwrap(this.peerNetData, buffer);
            var hsStatus = engineResult.getHandshakeStatus();
            var resultStatus = engineResult.getStatus();
            int bytesProduced = engineResult.bytesProduced();

            // store the buffer
            context.serverAppDataBuffers.add(buffer);

            logger.debug("Unwrap status: {} with {} bytes unwrapped", resultStatus, bytesProduced);

            peerNetData.compact();

            context.setState(bytesProduced > 0
                    ? ChannelState.HAS_APP_DATA
                    : ChannelState.CONTINUE_UNWRAP);

            return HandshakeProcessResult.fromSuccess(
                    handshakeStatus,
                    hsStatus,
                    resultStatus,
                    context
            );
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    };

    private boolean tryReadFromQueue(ByteBuffer buffer, Queue<ByteBuffer> queue) {
        if(queue.isEmpty()) {
            return false;
        }

        // we can safely pop the stack buffer
        do {
            // if the data in the stack is bigger than the buffer, peek it out and write to our buffer
            if(queue.peek().remaining() > buffer.remaining()) {
                var stackBuffer = queue.peek();

                var target = stackBuffer.slice().limit(stackBuffer.position() + buffer.remaining());
                stackBuffer.position(stackBuffer.position() + target.position()); // increment position manually
                buffer.put(target);
            }
            else {
                // the stack buffer is smaller or equal to our buffer, we can safely put it into
                var stackBuffer = queue.poll();
                buffer.put(stackBuffer); // will increment pos of stackBuffer

                // reuse this buffer for later reads
                cacheBuffer(stackBuffer);
            }
        }
        while (!queue.isEmpty());

        return !buffer.hasRemaining();
    }

    protected ByteBuffer getDataBuffer() {
        if(allocCache.isEmpty()) {
            return ByteBuffer.allocateDirect(1024);
        }
        else return allocCache.poll();
    }

    private void cacheBuffer(ByteBuffer buffer) {
        buffer.clear();
        this.allocCache.add(buffer);
    }

}
