package com.edgedb.driver.binary.duplexers;

import com.edgedb.driver.async.CompletableHandlerFuture;
import com.edgedb.driver.binary.PacketSerializer;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.receivable.Receivable;
import com.edgedb.driver.binary.packets.sendables.Sendable;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.ssl.SSLAsynchronousSocketChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class ChannelDuplexer extends Duplexer {
    private static final Logger logger = LoggerFactory.getLogger(ChannelDuplexer.class);

    private static final int PACKET_HEADER_SIZE = 5;

    // TODO: config these
    private static final int READ_TIMEOUT = 5000;
    private static final int WRITE_TIMEOUT = 5000;

    private final EdgeDBBinaryClient client;
    private SSLAsynchronousSocketChannel channel;
    private final ByteBuffer headerBuffer;
    private final AtomicBoolean isDisconnected;
    private final Semaphore readSemaphore;


    public ChannelDuplexer(EdgeDBBinaryClient client) {
        this.client = client;
        this.headerBuffer = ByteBuffer.allocateDirect(PACKET_HEADER_SIZE);
        this.readSemaphore = new Semaphore(1);
        this.isDisconnected = new AtomicBoolean(false);
    }

    private static class TransientPacket {
        public final ServerMessageType type;
        public final int length;
        public final ByteBuffer packetData;

        private TransientPacket(ServerMessageType type, int length) {
            this.type = type;
            this.length = length - 4; // remove length of the 'length' field of the packet
            this.packetData = ByteBuffer.allocateDirect(this.length);
        }
    }

    @Override
    public CompletionStage<Receivable> readNextAsync() {
        return CompletableFuture
                .runAsync(() -> {
                    try {
                        this.readSemaphore.acquire();
                    } catch (InterruptedException e) {
                        throw new CompletionException(e);
                    }
                })
                .thenCompose((v) -> {
                    if(this.isDisconnected.get() || channel == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    var readTask = new CompletableHandlerFuture<Integer, Object>();

                    this.channel.read(this.headerBuffer, READ_TIMEOUT, TimeUnit.MILLISECONDS, null, readTask);

                    return readTask;
                })
                .thenCompose((state) -> {
                    if(state == null) {
                        return null;
                    }

                    // TODO: how should -1 be handled?
                    if(state.result != 5) {
                        logger.warn("Expected to read 5 bytes, but read {}", state.result);
                        return null;
                    }

                    var type = ServerMessageType.valueOf(this.headerBuffer.get());
                    var length = this.headerBuffer.getInt();

                    logger.debug("S->C : {} - {}b", type, length);

                    var readTask = new CompletableHandlerFuture<Integer, TransientPacket>();

                    var transientPacket = new TransientPacket(type, length);

                    this.channel.read(transientPacket.packetData, READ_TIMEOUT, TimeUnit.MILLISECONDS, transientPacket, readTask);

                    return readTask;
                })
                .thenApply((state) -> {
                    if(state == null) {
                        return null;
                    }

                    // TODO: handle partial reads & -1
                    if(state.attachment.length != state.result) {
                        logger.warn("Expected to read {} bytes, but read {}", state.attachment.length, state.result);
                        return null;
                    }

                    return PacketSerializer.deserialize(state.attachment.type, state.attachment.length, state.attachment.packetData);
                })
                .whenComplete((v,e) -> {
                    this.readSemaphore.release(); // TODO: release on timeout?
                });
    }

    @Override
    public CompletionStage<Void> sendAsync(Sendable packet, @Nullable Sendable... packets) throws SSLException {
        if(channel == null || isDisconnected.get()) {
            return client.reconnectAsync().thenCompose((v) -> {
                try {
                    return this.sendAsync(packet, packets);
                } catch (SSLException e) {
                    throw new CompletionException(e);
                }
            }); // TODO: check for recursive loop
        }

        ByteBuffer data;
        try {
            data = PacketSerializer.serialize(packet, packets);
        } catch (OperationNotSupportedException e) {
            logger.error("Failed to serialize packets", e);
            return CompletableFuture.failedFuture(e);
        }

        var writeTask = new CompletableHandlerFuture<Integer, Object>();
        this.channel.write(data, WRITE_TIMEOUT, TimeUnit.MILLISECONDS, null, writeTask);
        return writeTask
                .thenAccept((v) -> {
                    // TODO: handle partial writes
                    if(v.result != data.limit()) {
                        logger.error("Failed to write entire buffer");
                    }
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

    @Override
    public void reset() {

    }

    public void init(SSLAsynchronousSocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public CompletableFuture<Void> disconnectAsync() {
        return null;
    }
}
