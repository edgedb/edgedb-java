package com.edgedb.driver.binary.protocol;

import com.edgedb.driver.GelConnection;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.protocol.v1.V1ProtocolProvider;
import com.edgedb.driver.binary.protocol.v2.V2ProtocolProvider;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.exceptions.MissingCodecException;
import com.edgedb.driver.exceptions.UnexpectedMessageException;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public interface ProtocolProvider {
    @Nullable ProtocolProvider DEFAULT_PROVIDER = null;

    ConcurrentMap<GelConnection, Function<EdgeDBBinaryClient, ProtocolProvider>> PROVIDERS_FACTORY = new ConcurrentHashMap<>();
    Map<ProtocolVersion, Function<EdgeDBBinaryClient, ProtocolProvider>> PROVIDERS = new HashMap<>(){{
       put(ProtocolVersion.of(1, 0), V1ProtocolProvider::new);
       put(ProtocolVersion.of(2, 0), V2ProtocolProvider::new);
    }};

    static ProtocolProvider getProvider(EdgeDBBinaryClient client) {
        return PROVIDERS_FACTORY.computeIfAbsent(
                client.getConnectionArguments(),
                ignored -> PROVIDERS.get(ProtocolVersion.BINARY_PROTOCOL_DEFAULT_VERSION)
        ).apply(client);
    }

    static void updateProviderFor(EdgeDBBinaryClient client, ProtocolProvider provider) {
        PROVIDERS_FACTORY.put(client.getConnectionArguments(), PROVIDERS.get(provider.getVersion()));
    }


    ProtocolVersion getVersion();
    ProtocolPhase getPhase();
    Map<String, @Nullable Object> getServerConfig();

    Receivable readPacket(ServerMessageType type, int length, PacketReader reader) throws UnexpectedMessageException;
    TypeDescriptorInfo<? extends Enum<?>> readDescriptor(PacketReader reader) throws UnexpectedMessageException;
    <T extends Enum<T>> @Nullable Codec<?> buildCodec(
            TypeDescriptorInfo<T> descriptor,
            Function<Integer, Codec<?>> getRelativeCodec, Function<Integer, TypeDescriptorInfo<?>> getRelativeDescriptor
    ) throws MissingCodecException;


    CompletionStage<ParseResult> parseQuery(QueryParameters queryParameters);
    CompletionStage<ExecuteResult> executeQuery(QueryParameters queryParameters, ParseResult parseResult);

    CompletionStage<Void> sendSyncMessage();
    CompletionStage<Void> processMessage(Receivable packet);

    Sendable handshake();
    Sendable terminate();
    Sendable sync();
}
