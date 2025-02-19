package com.gel.driver.binary;

import com.gel.driver.binary.protocol.ServerMessageType;
import com.gel.driver.binary.protocol.Receivable;
import com.gel.driver.binary.protocol.Sendable;
import com.gel.driver.clients.GelBinaryClient;
import com.gel.driver.exceptions.ConnectionFailedException;
import com.gel.driver.exceptions.GelException;
import com.gel.driver.util.HexUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

public class PacketSerializer {
    private static final Logger logger = LoggerFactory.getLogger(PacketSerializer.class);
    private static final Map<Class<?>, Map<Number, Enum<?>>> binaryEnumMap = new HashMap<>();

    public static <T extends Enum<?> & BinaryEnum<U>, U extends Number> void registerBinaryEnum(Class<T> cls, T @NotNull [] values) {
        binaryEnumMap.put(cls, Arrays.stream(values).collect(Collectors.toMap(BinaryEnum::getValue, v -> v)));
    }

    public static <T extends Enum<T> & BinaryEnum<U>, U extends Number> T getEnumValue(@NotNull Class<T> enumCls, U raw) {
        if(!binaryEnumMap.containsKey(enumCls)) {
            registerBinaryEnum(enumCls, enumCls.getEnumConstants());
        }

        //noinspection unchecked
        return (T)binaryEnumMap.get(enumCls).get(raw);
    }

    public static @NotNull MessageToMessageDecoder<ByteBuf> createDecoder(GelBinaryClient client) {
        return new MessageToMessageDecoder<>() {
            private final Map<Channel, PacketContract> contracts = new HashMap<>();

            @Override
            protected void decode(@NotNull ChannelHandlerContext ctx, @NotNull ByteBuf msg, @NotNull List<Object> out) throws Exception {
                var fromContract = false;

                if(contracts.containsKey(ctx.channel())){
                    var contract = contracts.get(ctx.channel());

                    logger.debug("Attempting to complete contract {}", contract);

                    if (contract.tryComplete(msg)) {
                        logger.debug("Contract completed of type {} with size {}", contract.messageType, contract.length);

                        out.add(contract.getPacket());
                        contracts.remove(ctx.channel());
                        fromContract = true;
                        msg = contract.data;
                    } else {
                        logger.debug("Contract pending [{}]: {}/{}", contract.messageType, contract.getSize(), contract.length);
                        return;
                    }
                }

                while (msg.readableBytes() > 5) {
                    var type = getEnumValue(ServerMessageType.class, msg.readByte());
                    var length = msg.readUnsignedInt() - 4; // remove length of self.

                    // can we read this packet?
                    if (msg.readableBytes() >= length) {
                        var packet = PacketSerializer.deserialize(client, type, length, msg.readSlice((int) length));

                        if(packet == null) {
                            logger.error("Got null result for packet type {}", type);
                            throw new GelException("Failed to read message type: malformed data");
                        }

                        logger.debug("S->C: T:{}", type);
                        out.add(packet);
                        continue;
                    }

                    // if we cannot read the full packet, create a contract for it.
                    msg.retain();
                    contracts.put(ctx.channel(), new PacketContract(msg, type, length));
                    return;
                }

                if(msg.readableBytes() > 0){
                    msg.retain();
                    contracts.put(ctx.channel(), new PacketContract(msg, null, null));
                    return;
                }

                if(fromContract){
                    msg.release();
                }
            }

            class PacketContract {
                private @Nullable Receivable packet;
                private ByteBuf data;

                private @Nullable ServerMessageType messageType;
                private @Nullable Long length;

                private final List<ByteBuf> components;

                public PacketContract(
                        ByteBuf data,
                        @Nullable ServerMessageType messageType,
                        @Nullable Long length
                ) {
                    this.data = data;
                    this.length = length;
                    this.messageType = messageType;

                    this.components = new ArrayList<>() {{
                        add(data);
                    }};
                }

                public long getSize() {
                    long size = 0;

                    for (var component : components) {
                        size += component.readableBytes();
                    }

                    return size;
                }

                public boolean tryComplete(@NotNull ByteBuf other) {
                    var orig = data.slice();
                    data = Unpooled.wrappedBuffer(orig, other);

                    if (messageType == null) {
                        messageType = getEnumValue(ServerMessageType.class, data.readByte());
                    }

                    if (length == null) {
                        length = data.readUnsignedInt() - 4;
                    }

                    other.retain();
                    components.add(other);

                    if (data.readableBytes() >= length) {
                        // read
                        packet = PacketSerializer.deserialize(client, messageType, length, data, false);

                        return true;
                    }

                    return false;
                }

                public @NotNull Receivable getPacket() throws OperationNotSupportedException {
                    if (packet == null) {
                        throw new OperationNotSupportedException("Packet contract was incomplete");
                    }

                    return packet;
                }
            }
        };
    }

    public static @NotNull MessageToMessageEncoder<Sendable> createEncoder() {
        return new MessageToMessageEncoder<>() {

            @Override
            protected void encode(@NotNull ChannelHandlerContext ctx, @NotNull Sendable msg, @NotNull List<Object> out) {

                try {
                    var data = PacketSerializer.serialize(msg);

                    data.readerIndex(0);

                    logger.debug("C->S: T:{} D:{}", msg.type, HexUtils.bufferToHexString(data));

                    out.add(data);
                } catch (Throwable x) {
                    logger.error("Failed to serialize packet", x);
                    ctx.fireExceptionCaught(x);
                    ctx.fireUserEventTriggered("DISCONNECT");
                }
            }
        };
    }

    public static @Nullable Receivable deserialize(
            GelBinaryClient client, ServerMessageType messageType, long length, @NotNull ByteBuf buffer
    ) {
        var reader = new PacketReader(buffer);
        return deserializeSingle(client, messageType, length, reader, true);
    }

    public static @Nullable Receivable deserialize(
            GelBinaryClient client, ServerMessageType messageType, long length, @NotNull ByteBuf buffer, boolean verifyEmpty
    ) {
        var reader = new PacketReader(buffer);
        return deserializeSingle(client, messageType, length, reader, verifyEmpty);
    }

    public static @Nullable Receivable deserializeSingle(GelBinaryClient client, PacketReader reader) {
        var messageType = reader.readEnum(ServerMessageType.class, Byte.TYPE);
        var length = reader.readUInt32().longValue();

        return deserializeSingle(client, messageType, length, reader, false);
    }

    public static @Nullable Receivable deserializeSingle(
            GelBinaryClient client, ServerMessageType type, long length, @NotNull PacketReader reader,
            boolean verifyEmpty
    ) {
        try {
            return client.getProtocolProvider().readPacket(type, (int)length, reader);
        }
        catch (Exception x) {
            logger.error("Failed to deserialize packet", x);
            return null;
        }
        finally {
            // ensure we read the entire packet
            if(verifyEmpty && !reader.isEmpty()) {
                logger.warn("Hanging data left inside packet reader of type {} with length {}", type, length);
            }
        }
    }

    public static HttpResponse.BodyHandler<List<Receivable>> createHandler(GelBinaryClient client) {
        return new PacketBodyHandler(client);
    }
    private static class PacketBodyHandler implements HttpResponse.BodyHandler<List<Receivable>> {
        private final GelBinaryClient client;
        public PacketBodyHandler(GelBinaryClient client) {
            this.client = client;
        }

        @Override
        public HttpResponse.BodySubscriber<List<Receivable>> apply(HttpResponse.ResponseInfo responseInfo) {
            // ensure success
            var isSuccess = responseInfo.statusCode() / 100 == 2;

            return isSuccess
                    ? new PacketBodySubscriber()
                    : new PacketBodySubscriber(responseInfo.statusCode());
        }

        private class PacketBodySubscriber implements HttpResponse.BodySubscriber<List<Receivable>> {
            private final @Nullable List<@NotNull ByteBuf> buffers;
            private final CompletableFuture<List<Receivable>> promise;

            public PacketBodySubscriber(int errorCode) {
                buffers = null;
                promise = CompletableFuture.failedFuture(
                        new ConnectionFailedException("Got HTTP error code " + errorCode)
                );
            }

            public PacketBodySubscriber() {
                promise = new CompletableFuture<>();
                buffers = new ArrayList<>();
            }

            @Override
            public CompletionStage<List<Receivable>> getBody() {
                return promise;
            }

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                if(buffers == null) {
                    return; // failed
                }

                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(List<ByteBuffer> items) {
                if(buffers == null) {
                    return; // failed
                }

                for(var item : items) {
                    buffers.add(Unpooled.wrappedBuffer(item));
                }
            }

            @Override
            public void onError(Throwable throwable) {
                promise.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                if(buffers == null) {
                    return; // failed
                }

                var completeBuffer = Unpooled.wrappedBuffer(buffers.toArray(new ByteBuf[0]));

                var reader = new PacketReader(completeBuffer);
                var data = new ArrayList<Receivable>();

                while(completeBuffer.readableBytes() > 0) {
                    var packet = deserializeSingle(client, reader);

                    if(packet == null && completeBuffer.readableBytes() > 0) {
                        promise.completeExceptionally(
                                new GelException("Failed to deserialize packet, buffer had " + completeBuffer.readableBytes() + " bytes remaining")
                        );
                        return;
                    }

                    data.add(packet);
                }

                promise.complete(data);
            }
        }
    }

    public static ByteBuf serialize(@NotNull Sendable packet, @Nullable Sendable @Nullable ... packets) throws OperationNotSupportedException {
        int size = packet.getSize();

        if(packets != null && packets.length > 0) {
            size += Arrays.stream(packets)
                    .filter(Objects::nonNull)
                    .mapToInt(Sendable::getSize)
                    .sum();
        }

        try (var writer = new PacketWriter(size)) {
            packet.write(writer);

            if(packets != null) {
                for (var p : packets) {
                    assert p != null;
                    p.write(writer);
                }
            }

            return writer.getBuffer();
        }
    }
}
