package com.edgedb.driver.binary.duplexers;

import com.edgedb.driver.binary.PacketSerializer;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.receivable.Receivable;
import com.edgedb.driver.binary.packets.sendables.Sendable;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class SocketDuplexer extends Duplexer {
    private static final Logger logger = LoggerFactory.getLogger(SocketDuplexer.class);

    private static final int PACKET_HEADER_SIZE = 5;

    // TODO: config these
    private static final int READ_TIMEOUT = 5000;
    private static final int WRITE_TIMEOUT = 5000;

    private final EdgeDBBinaryClient client;
    private SSLSocket socket;
    private OutputStream writeBuffer;
    private InputStream readBuffer;
    private final AtomicBoolean isDisconnected;
    private final Semaphore readSemaphore;

    private final byte[] packetHeader;
    private final ByteBuffer packetHeaderBuffer;

    public SocketDuplexer(EdgeDBBinaryClient client) {
        this.client = client;
        this.readSemaphore = new Semaphore(1);
        this.isDisconnected = new AtomicBoolean(false);
        packetHeader = new byte[5];
        packetHeaderBuffer = ByteBuffer.wrap(packetHeader);
    }

    private class TransientPacket {
        public final ServerMessageType type;
        public final int length;
        public final ByteBuffer packetData;
        public final byte[] rawPacketBuffer;

        public int count;

        private TransientPacket(ServerMessageType type, int length) {
            this.type = type;
            this.length = length - 4; // remove length of the 'length' field of the packet
            this.rawPacketBuffer = new byte[length];

            this.packetData = ByteBuffer.wrap(rawPacketBuffer);
        }

        public TransientPacket read() throws IOException {
            this.count = readBuffer.read(this.rawPacketBuffer);
            return this;
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
                .thenApply((v) -> {
                    if(this.isDisconnected.get() || socket == null) {
                        return -1;
                    }

                    try {
                        return readBuffer.read(this.packetHeader);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }

                })
                .thenApply((result) -> {
                    if(result == -1) {
                        return null;
                    }

                    var type = ServerMessageType.valueOf(this.packetHeaderBuffer.get());
                    var length = this.packetHeaderBuffer.getInt();

                    logger.debug("S->C : {} - {}b", type, length);

                    var transientPacket = new TransientPacket(type, length);

                    try {
                        return transientPacket.read();
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                })
                .thenApply((result) -> {
                    if(result == null)
                        return null;

                    if(result.count != result.rawPacketBuffer.length) {
                        logger.warn("Expected to read {} bytes, but read {}", result.rawPacketBuffer.length, result.count);
                    }

                    return PacketSerializer.deserialize(result.type, result.count, result.packetData);
                })
                .whenComplete((v,e) -> {
                    this.readSemaphore.release(); // TODO: release on timeout?
                });
    }

    @Override
    public CompletionStage<Void> sendAsync(Sendable packet, @Nullable Sendable... packets) throws SSLException {
        return CompletableFuture.supplyAsync(() -> {
            if(socket == null || isDisconnected.get()) {
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
                return null;
            }

            data.flip();

            try {
                this.writeBuffer.write(data.array());
            } catch (IOException e) {
                throw new CompletionException(e);
            }

            return data;
        }).thenCompose((v) -> {
            if(v instanceof CompletionStage<?>) {
                //noinspection unchecked
                return (CompletionStage<Void>)v;
            } else if (v instanceof ByteBuffer) {
                var b = (ByteBuffer)v;

                // TODO: handle partial writes
                if(b.hasRemaining()) {
                    logger.error("Failed to write entire buffer");
                }
            }

            return CompletableFuture.completedFuture(null);
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

    public void init(SSLSocket socket) throws IOException {
        this.socket = socket;
        this.writeBuffer = socket.getOutputStream();
        this.readBuffer = socket.getInputStream();
    }

    @Override
    public CompletableFuture<Void> disconnectAsync() {
        return null;
    }
}
