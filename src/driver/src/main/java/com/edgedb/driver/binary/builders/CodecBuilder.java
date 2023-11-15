package com.edgedb.driver.binary.builders;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.codecs.NullCodec;
import com.edgedb.driver.binary.codecs.scalars.*;
import com.edgedb.driver.binary.codecs.scalars.complex.DateTimeCodec;
import com.edgedb.driver.binary.codecs.scalars.complex.RelativeDurationCodec;
import com.edgedb.driver.binary.protocol.ProtocolProvider;
import com.edgedb.driver.ProtocolVersion;
import com.edgedb.driver.binary.protocol.TypeDescriptorInfo;
import com.edgedb.driver.binary.protocol.common.Cardinality;
import com.edgedb.driver.binary.protocol.common.IOFormat;
import com.edgedb.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.exceptions.MissingCodecException;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public final class CodecBuilder {
    private static final class CodecCache {
        public final ConcurrentMap<UUID, Codec<?>> cache;
        public final ConcurrentMap<UUID, Codec<?>> codecPartsInstanceCache;
        public final ConcurrentMap<Long, QueryCodecCacheEntry> queryCodecsCache;

        public final ProtocolVersion version;


        private CodecCache(ProtocolVersion version) {
            this.version = version;
            this.cache = new ConcurrentHashMap<>(16);
            this.codecPartsInstanceCache = new ConcurrentHashMap<>(16);
            this.queryCodecsCache = new ConcurrentHashMap<>(8);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(CodecBuilder.class);

    public static final UUID NULL_CODEC_ID = new UUID(0L, 0L);
    public static final UUID INVALID_CODEC_ID = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);

    private static final ConcurrentMap<ProtocolVersion, CodecCache> codecCaches;

    static {
        codecCaches = new ConcurrentHashMap<>(2);
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable Codec<T> getCodec(ProtocolProvider provider, UUID id, Class<T> ignoredCls) {
        return (Codec<T>) getCodec(provider, id);
    }
    public static @Nullable Codec<?> getCodec(ProtocolProvider provider, UUID id) {
        var codec = codecCaches.computeIfAbsent(provider.getVersion(), CodecCache::new).cache.get(id);
        return codec != null ? codec : getCachedOrScalarCodec(provider, id);
    }

    public static @NotNull Codec<?> buildCodec(EdgeDBBinaryClient client, @NotNull UUID id, @Nullable ByteBuf buffer) throws EdgeDBException {
        if(id.equals(NULL_CODEC_ID) || buffer == null) {
            return getOrCreateCodec(client.getProtocolProvider(), NULL_CODEC_ID, NullCodec::new);
        }

        var reader = new PacketReader(buffer);
        return buildCodec(client, id, reader);
    }

    @SuppressWarnings("unchecked")
    public static <T> @NotNull Codec<T> buildCodec(EdgeDBBinaryClient client, @NotNull UUID id, @Nullable ByteBuf buffer, Class<T> cls) throws EdgeDBException, OperationNotSupportedException {
        if(id.equals(NULL_CODEC_ID) || buffer == null) {
            return (Codec<T>)getOrCreateCodec(client.getProtocolProvider(), NULL_CODEC_ID, NullCodec::new);
        }

        var reader = new PacketReader(buffer);
        return buildCodec(client, id, reader, cls);
    }

    @SuppressWarnings("unchecked")
    public static <T> @NotNull Codec<T> buildCodec(EdgeDBBinaryClient client, @NotNull UUID id, @NotNull PacketReader reader, Class<T> ignoredCodecResult) throws EdgeDBException {
        return (Codec<T>) buildCodec(client, id, reader);
    }

    public static @NotNull Codec<?> buildCodec(EdgeDBBinaryClient client, @NotNull UUID id, @NotNull PacketReader reader) throws EdgeDBException {
        try {
            if(id.equals(NULL_CODEC_ID)) {
                logger.debug("Returning null codec");
                return getOrCreateCodec(client.getProtocolProvider(), id, NullCodec::new);
            }

            var providerCache = codecCaches.computeIfAbsent(client.getProtocolProvider().getVersion(), CodecCache::new);

            var descriptors = new ArrayList<TypeDescriptorInfo<? extends Enum<?>>>();

            while(!reader.isEmpty()) {
                var start = reader.position();
                var descriptor = client.getProtocolProvider().readDescriptor(reader);
                var end = reader.position();

                logger.trace("{}/{}: read {}, size {}", end, reader.size(), descriptor.type, end-start);

                descriptors.add(descriptor);
            }

            logger.debug("Read {} descriptors, totaling {} bytes", descriptors.size(), reader.position());

            var codecs = new ArrayList<Codec<?>>();

            for(var i = 0; i != descriptors.size(); i++) {
                var descriptor = descriptors.get(i);

                Codec<?> codec = providerCache.cache.get(descriptor.getId());;

                if(codec != null) {
                    logger.debug("Using cached codec {} from ID: {}", codec, descriptor.getId());
                    codecs.add(i, codec);
                    continue;
                }

                codec = getCachedOrScalarCodec(client.getProtocolProvider(), descriptor.getId());

                if(codec != null) {
                    logger.debug("Using cached codec {} from ID: {}", codec, descriptor.getId());
                    codecs.add(i, codec);
                    continue;
                }

                logger.debug("Calling protocol provider for codec construction, descriptor type: {}", descriptor.type);

                codec = client.getProtocolProvider().buildCodec(
                        descriptor,
                        codecs::get,
                        descriptors::get
                );

                logger.debug("Protocol provider returned {}", codec == null ? "null" : codec);

                codecs.add(i, codec);

                logger.debug("Codec {} added: {}, ID: {}", i, codec, descriptor.getId());
            }

            Codec<?> finalCodec = null;

            for(var i = 1; i != codecs.size() + 1 && finalCodec == null; i++) {
                finalCodec = codecs.get(codecs.size() - i);
            }

            if(finalCodec == null) {
                throw new MissingCodecException("Failed to find end tail of codec tree");
            }

            return finalCodec;
        }
        catch (Throwable x) {
            logger.error("Failed to build codec", x);
            throw x;
        }
    }

    public static @NotNull Long getCacheKey(@NotNull String query, @NotNull Cardinality cardinality, @NotNull IOFormat format) {
        return calculateKnuthHash(query) + cardinality.getValue() + format.getValue();
    }

    public static @Nullable QueryCodecs getCachedCodecs(ProtocolProvider provider, long cacheKey) {
        var providerCache = codecCaches.computeIfAbsent(provider.getVersion(), CodecCache::new);

        var ids = providerCache.queryCodecsCache.get(cacheKey);

        if(ids == null) {
            return null;
        }

        var inCodec = providerCache.cache.get(ids.inputCodecId);
        var outCodec = providerCache.cache.get(ids.outputCodecId);

        if(inCodec == null || outCodec == null) {
            providerCache.queryCodecsCache.remove(cacheKey);
            return null;
        }

        return new QueryCodecs(ids.inputCodecId, inCodec, ids.outputCodecId, outCodec);
    }

    public static void updateCachedCodecs(ProtocolProvider provider, long cacheKey, UUID inCodecId, UUID outCodecId) {
        codecCaches.computeIfAbsent(provider.getVersion(), CodecCache::new)
                .queryCodecsCache.computeIfAbsent(cacheKey, (c) -> new QueryCodecCacheEntry(inCodecId, outCodecId));
    }

    private static @NotNull Long calculateKnuthHash(@NotNull String str) {
        var h = 3074457345618258791L;

        for(int i = 0; i != str.length(); i++) {
            h += str.charAt(i);
            h *= 3074457345618258799L;
        }

        return h;
    }

    @SuppressWarnings("unchecked")
    public static <T> Codec<T> getOrCreateCodec(
            ProtocolProvider provider,
            UUID id,
            @NotNull Supplier<Codec<T>> constructor
    ) {
        return (Codec<T>) codecCaches.computeIfAbsent(provider.getVersion(), CodecCache::new)
                .codecPartsInstanceCache.computeIfAbsent(id, ignored -> constructor.get());
    }

    @SuppressWarnings("unchecked")
    public static <T> Codec<T> getOrCreateCodec(
            ProtocolProvider provider,
            UUID id,
            @Nullable CodecMetadata metadata,
            @NotNull Function<@Nullable CodecMetadata, Codec<T>> constructor
    ) {
        return (Codec<T>) codecCaches.computeIfAbsent(provider.getVersion(), CodecCache::new)
                .codecPartsInstanceCache.computeIfAbsent(id, ignored -> constructor.apply(metadata));
    }

    @SuppressWarnings("unchecked")
    public static <T> Codec<T> getOrCreateCodec(
            ProtocolProvider provider,
            UUID id,
            @Nullable CodecMetadata metadata,
            @NotNull BiFunction<UUID, @Nullable CodecMetadata, Codec<T>> constructor
    ) {
        if(logger.isDebugEnabled()) {
            logger.debug(
                    "cache requested id: {}. exists?: {}, metadata: {}",
                    id,
                    codecCaches
                            .computeIfAbsent(provider.getVersion(), CodecCache::new)
                            .codecPartsInstanceCache.containsKey(id),
                    metadata == null
                            ? "none"
                            : metadata.toString()
            );
        }

        return (Codec<T>) codecCaches.computeIfAbsent(provider.getVersion(), CodecCache::new)
                .codecPartsInstanceCache.computeIfAbsent(id, i -> constructor.apply(i, metadata));
    }

    @SuppressWarnings("unchecked")
    private static <T> @Nullable Codec<T> getCachedOrScalarCodec(ProtocolProvider provider, UUID id) {
        return (Codec<T>) codecCaches.computeIfAbsent(provider.getVersion(), CodecCache::new)
                .codecPartsInstanceCache.computeIfAbsent(
                        id,
                        (v) -> scalarCodecFactories.containsKey(v)
                            ? scalarCodecFactories.get(v).apply(null)
                            : null
        );
    }

    private static final Map<UUID, Function<@Nullable CodecMetadata, Codec<?>>> scalarCodecFactories = new HashMap<>() {
        {
            put(UUID.fromString("00000000-0000-0000-0000-000000000100"), UUIDCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-000000000101"), TextCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-000000000102"), BytesCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-000000000103"), Integer16Codec::new);
            put(UUID.fromString("00000000-0000-0000-0000-000000000104"), Integer32Codec::new);
            put(UUID.fromString("00000000-0000-0000-0000-000000000105"), Integer64Codec::new);
            put(UUID.fromString("00000000-0000-0000-0000-000000000106"), Float32Codec::new);
            put(UUID.fromString("00000000-0000-0000-0000-000000000107"), Float64Codec::new);
            put(UUID.fromString("00000000-0000-0000-0000-000000000108"), DecimalCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-000000000109"), BoolCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-00000000010A"), DateTimeCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-00000000010B"), LocalDateTimeCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-00000000010C"), LocalDateCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-00000000010D"), LocalTimeCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-00000000010E"), DurationCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-00000000010F"), JsonCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-000000000110"), BigIntCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-000000000111"), RelativeDurationCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-000000000112"), DateDurationCodec::new);
            put(UUID.fromString("00000000-0000-0000-0000-000000000130"), MemoryCodec::new);
        }
    };

    @SuppressWarnings("rawtypes")
    public static final class QueryCodecs {
        public final UUID inputCodecId;
        public final Codec inputCodec;
        public final UUID outputCodecId;
        public final Codec outputCodec;

        public QueryCodecs(UUID inputCodecId, Codec inputCodec, UUID outputCodecId, Codec outputCodec) {
            this.inputCodecId = inputCodecId;
            this.inputCodec = inputCodec;
            this.outputCodecId = outputCodecId;
            this.outputCodec = outputCodec;
        }
    }

    private static final class QueryCodecCacheEntry {
        public final UUID inputCodecId;
        public final UUID outputCodecId;

        private QueryCodecCacheEntry(UUID inputCodecId, UUID outputCodecId) {
            this.inputCodecId = inputCodecId;
            this.outputCodecId = outputCodecId;
        }
    }
}
