package com.edgedb.driver.binary;

import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.receivable.*;
import com.edgedb.driver.binary.packets.sendables.Sendable;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;
import java.util.*;
import java.util.function.Function;

public class PacketSerializer {
    private static final Logger logger = LoggerFactory.getLogger(PacketSerializer.class);
    private static final Map<ServerMessageType, Function<PacketReader, Receivable>> deserializerMap;

    static {
        deserializerMap = new HashMap<>();

        deserializerMap.put(ServerMessageType.AUTHENTICATION, AuthenticationStatus::new);
        deserializerMap.put(ServerMessageType.COMMAND_COMPLETE, CommandComplete::new);
        deserializerMap.put(ServerMessageType.COMMAND_DATA_DESCRIPTION, CommandDataDescription::new);
        deserializerMap.put(ServerMessageType.DATA, Data::new);
        deserializerMap.put(ServerMessageType.DUMP_BLOCK, DumpBlock::new);
        deserializerMap.put(ServerMessageType.DUMP_HEADER, DumpHeader::new);
        deserializerMap.put(ServerMessageType.ERROR_RESPONSE, ErrorResponse::new);
        deserializerMap.put(ServerMessageType.LOG_MESSAGE, LogMessage::new);
        deserializerMap.put(ServerMessageType.PARAMETER_STATUS, ParameterStatus::new);
        deserializerMap.put(ServerMessageType.READY_FOR_COMMAND, ReadyForCommand::new);
        deserializerMap.put(ServerMessageType.RESTORE_READY, RestoreReady::new);
        deserializerMap.put(ServerMessageType.SERVER_HANDSHAKE, ServerHandshake::new);
        deserializerMap.put(ServerMessageType.SERVER_KEY_DATA, ServerKeyData::new);
        deserializerMap.put(ServerMessageType.STATE_DATA_DESCRIPTION, StateDataDescription::new);
    }

    public static final MessageToMessageDecoder<ByteBuf> PACKET_DECODER = new MessageToMessageDecoder<ByteBuf>() {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
            // TODO: packet contract should be implemented if the 'msg' is partial due to async IO.
            // packet contracts act as pub-sub in the sense that theres a queue of buffers, successfully
            // read packets, and completion sources for said packets. consumers pop out either A: a completed
            // packet contract; B: a packet contract which is partial and awaiting more data from IO; or C:
            // a completion stage that represents a unfulfilled contract that is completed on the next read.

            var type = ServerMessageType.valueOf(msg.readByte());
            var length = msg.readUnsignedInt() - 4; // remove length of self.
            var packet = PacketSerializer.deserialize(type, length, msg);
            out.add(packet);
        }
    };

    public static final MessageToMessageEncoder<Sendable> PACKET_ENCODER = new MessageToMessageEncoder<Sendable>() {

        @Override
        protected void encode(ChannelHandlerContext ctx, Sendable msg, List<Object> out) throws Exception {

            out.add(PacketSerializer.serialize(msg));
        }
    };

    public static Receivable deserialize(ServerMessageType messageType, long length, ByteBuf buffer) {
        var reader = new PacketReader(buffer);

        if(!deserializerMap.containsKey(messageType)) {
            logger.error("Unknown packet type {}", messageType);
            reader.skip((int)length);
            return null;
        }

        try {
            return deserializerMap.get(messageType).apply(reader);
        }
        catch (Exception x) {
            logger.error("Failed to deserialize packet", x);
            throw x;
        }
        finally {
            // ensure we read the entire packet
            if(!reader.isEmpty()) {
                logger.warn("Hanging data left inside packet reader of type {} with length {}", messageType, length);
            }
        }
    }

    public static ByteBuf serialize(@NotNull Sendable packet, @Nullable Sendable... packets) throws OperationNotSupportedException {
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
