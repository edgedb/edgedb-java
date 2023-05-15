package com.edgedb.driver.binary.builders;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.*;
import com.edgedb.driver.binary.codecs.scalars.*;
import com.edgedb.driver.binary.codecs.scalars.complex.BytesCodec;
import com.edgedb.driver.binary.codecs.scalars.complex.DateTimeCodec;
import com.edgedb.driver.binary.codecs.scalars.complex.RelativeDurationCodec;
import com.edgedb.driver.binary.descriptors.*;
import com.edgedb.driver.binary.packets.shared.Cardinality;
import com.edgedb.driver.binary.packets.shared.IOFormat;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.exceptions.MissingCodecException;
import com.edgedb.driver.util.CollectionUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public final class CodecBuilder {
    private static final Logger logger = LoggerFactory.getLogger(CodecBuilder.class);

    public static final UUID NULL_CODEC_ID = new UUID(0L, 0L);
    public static final UUID INVALID_CODEC_ID = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
    private static final ConcurrentMap<UUID, Codec<?>> codecPartsInstanceCache;
    private static final ConcurrentMap<UUID, Codec<?>> codecCache;
    private static final ConcurrentMap<Long, QueryCodecCacheEntry> queryCodecCache;

    static {
        codecPartsInstanceCache = new ConcurrentHashMap<>(16);
        codecCache = new ConcurrentHashMap<>(32);
        queryCodecCache = new ConcurrentHashMap<>(16);
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable Codec<T> getCodec(UUID id, Class<T> cls) {
        return (Codec<T>) getCodec(id);
    }
    public static @Nullable Codec<?> getCodec(UUID id) {
        var codec = codecCache.get(id);
        return codec != null ? codec : getScalarCodec(id);
    }

    public static Codec<?> buildCodec(EdgeDBBinaryClient client, UUID id, @Nullable ByteBuf buffer) throws EdgeDBException, OperationNotSupportedException {
        if(id.equals(NULL_CODEC_ID) || buffer == null) {
            return getOrCreateCodec(NULL_CODEC_ID, NullCodec::new);
        }

        var reader = new PacketReader(buffer);
        return buildCodec(client, id, reader);
    }

    @SuppressWarnings("unchecked")
    public static <T> Codec<T> buildCodec(EdgeDBBinaryClient client, UUID id, @Nullable ByteBuf buffer, Class<T> cls) throws EdgeDBException, OperationNotSupportedException {
        if(id.equals(NULL_CODEC_ID) || buffer == null) {
            return (Codec<T>)getOrCreateCodec(NULL_CODEC_ID, NullCodec::new);
        }

        var reader = new PacketReader(buffer);
        return buildCodec(client, id, reader, cls);
    }

    @SuppressWarnings("unchecked")
    public static <T> Codec<T> buildCodec(EdgeDBBinaryClient client, UUID id, PacketReader reader, Class<T> codecResult) throws EdgeDBException, OperationNotSupportedException {
        return (Codec<T>) buildCodec(client, id, reader);
    }
    @SuppressWarnings("unchecked")
    public static Codec<?> buildCodec(EdgeDBBinaryClient client, UUID id, PacketReader reader) throws EdgeDBException, OperationNotSupportedException {
        try {
            if(id.equals(NULL_CODEC_ID)) {
                return getOrCreateCodec(id, NullCodec::new);
            }

            List<Codec<?>> codecs = new ArrayList<>();

            while(!reader.isEmpty()) {
                var start = reader.position();
                var descriptor = TypeDescriptorBuilder.getDescriptor(reader);
                var end = reader.position();

                logger.trace("{}/{}: read {}, size {}", end, reader.size(), descriptor.type, end-start);

                Codec<?> codec;

                if(codecCache.containsKey(descriptor.getId())) {
                    codec = codecCache.get(descriptor.getId());
                } else {
                    codec = getScalarCodec(descriptor.getId());
                }

                if(codec != null) {
                    codecs.add(codec);
                    continue;
                }

                switch (descriptor.type) {
                    case ARRAY_TYPE_DESCRIPTOR:
                        var arrayType = descriptor.as(ArrayTypeDescriptor.class);

                        codec = getOrCreateCodec(descriptor.getId(), () ->
                                new CompilableCodec(
                                        descriptor.getId(),
                                        codecs.get(arrayType.typePosition.intValue()),
                                        ArrayCodec::new
                                )
                        );
                        break;
                    case SCALAR_TYPE_DESCRIPTOR:
                    case BASE_SCALAR_TYPE_DESCRIPTOR:
                        // should be resolved by the above case, getting here is an error
                        throw new MissingCodecException(String.format("Could not find the scalar type %s", descriptor.getId().toString()));
                    case ENUMERATION_TYPE_DESCRIPTOR:
                        codec = getOrCreateCodec(descriptor.getId(), TextCodec::new);
                        break;
                    case INPUT_SHAPE_DESCRIPTOR:
                        var inputShape = descriptor.as(InputShapeDescriptor.class);
                        var inputShapeCodecs = new Codec[inputShape.shapes.length];
                        var inputShapeNames = new String[inputShape.shapes.length];

                        for (int i = 0; i != inputShape.shapes.length; i++) {
                            inputShapeCodecs[i] = codecs.get(inputShape.shapes[i].typePosition.intValue());
                            inputShapeNames[i] = inputShape.shapes[i].name;
                        }

                        codec = getOrCreateCodec(descriptor.getId(), () -> new SparseObjectCodec(inputShapeCodecs, inputShapeNames));
                        break;


                    case TUPLE_TYPE_DESCRIPTOR:
                        var tupleType = descriptor.as(TupleTypeDescriptor.class);
                        var innerCodecs = new Codec<?>[tupleType.elementTypeDescriptorPositions.length];

                        for(int i = 0; i != innerCodecs.length; i++) {
                            innerCodecs[i] = codecs.get(tupleType.elementTypeDescriptorPositions[i].intValue());
                        }

                        codec = getOrCreateCodec(descriptor.getId(), () -> new TupleCodec(innerCodecs));
                        break;
                    case NAMED_TUPLE_DESCRIPTOR:
                        var tupleShape = descriptor.as(NamedTupleTypeDescriptor.class);
                        codec = getOrCreateCodec(descriptor.getId(), () -> ObjectCodec.create(codecs::get, tupleShape.elements));
                        break;
                    case OBJECT_SHAPE_DESCRIPTOR:
                        var objectShape = descriptor.as(ObjectShapeDescriptor.class);
                        codec = getOrCreateCodec(descriptor.getId(), () -> ObjectCodec.create(codecs::get, objectShape.shapes));
                        break;
                    case RANGE_TYPE_DESCRIPTOR:
                        var rangeType = descriptor.as(RangeTypeDescriptor.class);

                        codec = getOrCreateCodec(descriptor.getId(), () ->
                                new CompilableCodec(
                                        descriptor.getId(),
                                        codecs.get(rangeType.typePosition.intValue()),
                                        RangeCodec::new)
                        );
                        break;
                    case SCALAR_TYPE_NAME_ANNOTATION:
                        // TODO: should we do anything here?
                        break;
                    case SET_DESCRIPTOR:
                        var setTypes = descriptor.as(SetTypeDescriptor.class);

                        codec = getOrCreateCodec(descriptor.getId(), () ->
                                new CompilableCodec(
                                        descriptor.getId(),
                                        codecs.get(setTypes.typePosition.intValue()),
                                        SetCodec::new
                                )
                        );
                        break;
                    default:
                        throw new MissingCodecException(String.format("Could not find a type descriptor with the type %s", descriptor.getId().toString()));
                }

                codecs.add(codec);
            }

            var finalCodec = CollectionUtils.last(codecs);

            codecCache.putIfAbsent(id, finalCodec);

            return finalCodec;
        }
        catch (Throwable x) {
            logger.error("Failed to build codec", x);
            throw x;
        }

    }

    public static Long getCacheKey(String query, Cardinality cardinality, IOFormat format) {
        return calculateKnuthHash(query) + cardinality.getValue() + format.getValue();
    }

    public static @Nullable QueryCodecs getCachedCodecs(long cacheKey) {
        var ids = queryCodecCache.get(cacheKey);

        if(ids == null) {
            return null;
        }

        var inCodec = codecCache.get(ids.inputCodecId);
        var outCodec = codecCache.get(ids.outputCodecId);

        if(inCodec == null || outCodec == null) {
            queryCodecCache.remove(cacheKey);
            return null;
        }

        return new QueryCodecs(ids.inputCodecId, inCodec, ids.outputCodecId, outCodec);
    }

    public static void updateCachedCodecs(long cacheKey, UUID inCodecId, UUID outCodecId) {
        queryCodecCache.computeIfAbsent(cacheKey, (c) -> new QueryCodecCacheEntry(inCodecId, outCodecId));
    }

    private static Long calculateKnuthHash(String str) {
        var h = 3074457345618258791L;

        for(int i = 0; i != str.length(); i++) {
            h += str.charAt(i);
            h *= 3074457345618258799L;
        }

        return h;
    }

    @SuppressWarnings("unchecked")
    private static <T> Codec<T> getOrCreateCodec(UUID id, Supplier<Codec<T>> constructor) {
        return (Codec<T>) codecPartsInstanceCache.computeIfAbsent(id, v -> constructor.get());
    }

    @SuppressWarnings("unchecked")
    private static <T> @Nullable Codec<T> getScalarCodec(UUID id) {
        return (Codec<T>) codecPartsInstanceCache.computeIfAbsent(id,
                (v) -> scalarCodecFactories.containsKey(v)
                        ? scalarCodecFactories.get(v).get()
                        : null
        );
    }

    private static final Map<UUID, Supplier<Codec<?>>> scalarCodecFactories = new HashMap<>() {
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
