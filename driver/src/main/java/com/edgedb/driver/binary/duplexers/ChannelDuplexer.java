package com.edgedb.driver.binary.duplexers;

import com.edgedb.driver.async.AsyncSemaphore;
import com.edgedb.driver.binary.PacketSerializer;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.receivable.Receivable;
import com.edgedb.driver.binary.packets.sendables.Sendable;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.ssl.AsyncSSLChannel;
import com.edgedb.driver.util.BinaryProtocolUtils;
import com.edgedb.driver.util.HexUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class ChannelDuplexer extends Duplexer {
    private static final Logger logger = LoggerFactory.getLogger(ChannelDuplexer.class);

    private static final int PACKET_HEADER_SIZE = 5;

    // TODO: config these
    private static final int READ_TIMEOUT = 5000;
    private static final int WRITE_TIMEOUT = 5000;

    private AsyncSSLChannel channel;

    private final EdgeDBBinaryClient client;
    private final AtomicBoolean isDisconnected;
    private final AsyncSemaphore readSemaphore;
    private final ByteBuffer packetHeaderBuffer;

    public ChannelDuplexer(EdgeDBBinaryClient client) {
        this.client = client;
        this.readSemaphore = new AsyncSemaphore(1);
        this.isDisconnected = new AtomicBoolean(false);
        packetHeaderBuffer = ByteBuffer.allocateDirect(5);
    }

    @Override
    public CompletionStage<Receivable> readNextAsync() {
        return CompletableFuture
                .runAsync(readSemaphore::aquire)
                .thenCompose((v) -> {
                    if(this.isDisconnected.get() || this.channel == null) {
                        return CompletableFuture.completedFuture(-1);
                    }

                    return this.channel.readAsync(this.packetHeaderBuffer, READ_TIMEOUT, TimeUnit.MILLISECONDS);
                })
                .thenCompose((result) -> {
                    if(result == -1) {
                        return null;
                    }

                    this.packetHeaderBuffer.flip();

                    var type = ServerMessageType.valueOf(this.packetHeaderBuffer.get());
                    var length = this.packetHeaderBuffer.getInt() - BinaryProtocolUtils.INT_SIZE; // remove length of the 'length' in the protocol method

                    logger.debug("S->C : {} - {}b", type, length);

                    // TODO: shared memory pool
                    var buffer = ByteBuffer.allocateDirect(length);

                    return this.channel.readAsync(buffer, READ_TIMEOUT, TimeUnit.MILLISECONDS)
                            .thenApply((count) -> {
                                if(count != buffer.limit()) {
                                    logger.warn("Expected to read {} bytes, but read {}", buffer.limit(), count);
                                    return null;
                                }

                                return PacketSerializer.deserialize(type, length, buffer);
                            });
                })
                .whenComplete((v,e) -> {
                    this.readSemaphore.release(); // TODO: release on timeout?
                });
    }

    @Override
    public CompletionStage<Void> sendAsync(Sendable packet, @Nullable Sendable... packets) throws SSLException {
        logger.debug("Starting to send packets to {}, is connected? {}", channel, !isDisconnected.get());

        if(channel == null || isDisconnected.get()) {
            logger.debug("Reconnecting...");
            return client.reconnectAsync().thenCompose((v) -> {
                try {
                    logger.debug("Sending after reconnect");
                    return this.sendAsync(packet, packets);
                } catch (SSLException e) {
                    throw new CompletionException(e);
                }
            }); // TODO: check for recursive loop
        }

        ByteBuffer data;
        try {
            logger.debug("Serializing {} packets", 1 + (packets == null ? 0 : packets.length));
            data = PacketSerializer.serialize(packet, packets);
            logger.debug("Serialization complete: {} bytes", data.position());
        } catch (OperationNotSupportedException e) {
            logger.error("Failed to serialize packets", e);
            return null;
        }

        data.flip();

        if(logger.isDebugEnabled()) {
            var h = HexUtils.bufferToHexString(data);
            data.flip();
            logger.debug("Writing data to underlying channel... Data: {}", h);
        } else {
            logger.debug("Writing data to underlying channel...");
        }

        return this.channel.writeAsync(data, WRITE_TIMEOUT, TimeUnit.MILLISECONDS);
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

    public void init(AsyncSSLChannel channel) {
        this.channel = channel;
    }

    @Override
    public CompletableFuture<Void> disconnectAsync() {
        return null;
    }
}
