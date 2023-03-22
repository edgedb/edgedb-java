package com.edgedb.driver.binary.builders;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.*;
import com.edgedb.driver.binary.codecs.scalars.*;
import com.edgedb.driver.binary.codecs.scalars.complex.DateTimeCodec;
import com.edgedb.driver.binary.codecs.scalars.complex.RelativeDurationCodec;
import com.edgedb.driver.binary.descriptors.ArrayTypeDescriptor;
import com.edgedb.driver.binary.descriptors.InputShapeDescriptor;
import com.edgedb.driver.binary.descriptors.ObjectShapeDescriptor;
import com.edgedb.driver.binary.descriptors.RangeTypeDescriptor;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.exceptions.MissingCodecException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    static {
        codecPartsInstanceCache = new ConcurrentHashMap<>(16);
        codecCache = new ConcurrentHashMap<>(32);
    }

    public static Codec<?> buildCodec(EdgeDBBinaryClient client, UUID id, PacketReader reader) throws EdgeDBException {
        if(id.equals(NULL_CODEC_ID)) {
            return getOrCreateCodec(id, NullCodec::new);
        }

        List<Codec<?>> codecs = new ArrayList<>();

        while(!reader.isEmpty()) {
            var start = reader.position();
            var descriptor = TypeDescriptorBuilder.getDescriptor(reader);
            var end = reader.position();

            logger.trace("{}/{}: read {}, size {}", end, reader.size(), descriptor, end-start);

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
                    codec = new ArrayCodec<>(codecs.get(descriptor.as(ArrayTypeDescriptor.class).typePosition.intValue()));
                    break;
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
                case NAMED_TUPLE_DESCRIPTOR:
                case TUPLE_TYPE_DESCRIPTOR:
                    // TODO
                    break;
                case OBJECT_SHAPE_DESCRIPTOR:
                    var objectShape = descriptor.as(ObjectShapeDescriptor.class);
                    var objectShapeCodecs = new Codec[objectShape.shapes.length];
                    var objectShapeNames = new String[objectShape.shapes.length];

                    for (int i = 0; i != objectShape.shapes.length; i++) {
                        objectShapeCodecs[i] = codecs.get(objectShape.shapes[i].typePosition.intValue());
                        objectShapeNames[i] = objectShape.shapes[i].name;
                    }

                    codec = getOrCreateCodec(descriptor.getId(), () -> new ObjectCodec(objectShapeCodecs, objectShapeNames));
                    break;
                case RANGE_TYPE_DESCRIPTOR:
                    var rangeType = descriptor.as(RangeTypeDescriptor.class);
                    codec = getOrCreateCodec(descriptor.getId(), () -> new RangeCodec<>())
            }

        }
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
}
