package com.edgedb.driver.binary.protocol.v2;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.builders.CodecBuilder;
import com.edgedb.driver.binary.codecs.ArrayCodec;
import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.codecs.CompilableCodec;
import com.edgedb.driver.binary.protocol.ProtocolProvider;
import com.edgedb.driver.binary.protocol.ProtocolVersion;
import com.edgedb.driver.binary.protocol.TypeDescriptor;
import com.edgedb.driver.binary.protocol.TypeDescriptorInfo;
import com.edgedb.driver.binary.protocol.v1.V1ProtocolProvider;
import com.edgedb.driver.binary.protocol.v2.descriptors.*;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.exceptions.MissingCodecException;
import com.edgedb.driver.exceptions.UnexpectedMessageException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

public class V2ProtocolProvider extends V1ProtocolProvider implements ProtocolProvider {
    private static final Logger logger = LoggerFactory.getLogger(V1ProtocolProvider.class);

    private static final Map<DescriptorType, BiFunction<UUID, PacketReader, ? extends TypeDescriptor>> TYPE_DESCRIPTOR_MAP;

    static {
        TYPE_DESCRIPTOR_MAP = new HashMap<>(){{
            put(DescriptorType.ARRAY, ArrayTypeDescriptor::new);
            put(DescriptorType.COMPOUND, CompoundTypeDescriptor::new);
            put(DescriptorType.ENUMERATION, EnumerationTypeDescriptor::new);
            put(DescriptorType.INPUT, InputShapeDescriptor::new);
            put(DescriptorType.NAMED_TUPLE, NamedTupleTypeDescriptor::new);
            put(DescriptorType.OBJECT_OUTPUT, ObjectOutputShapeDescriptor::new);
            put(DescriptorType.OBJECT, ObjectTypeDescriptor::new);
            put(DescriptorType.RANGE, RangeTypeDescriptor::new);
            put(DescriptorType.SCALAR, ScalarTypeDescriptor::new);
            put(DescriptorType.SET, SetDescriptor::new);
            put(DescriptorType.TUPLE, TupleTypeDescriptor::new);
            put(DescriptorType.TYPE_ANNOTATION_TEXT, (ignored, reader) -> new TypeAnnotationTextDescriptor(reader));
        }};
    }

    @Override
    public ProtocolVersion getVersion() {
        return ProtocolVersion.of(2, 0);
    }

    public V2ProtocolProvider(EdgeDBBinaryClient client) {
        super(client);
    }

    @Override
    public TypeDescriptorInfo<DescriptorType> readDescriptor(PacketReader reader) throws UnexpectedMessageException {
        var type = reader.readEnum(DescriptorType.class, Byte.TYPE);

        var id = type == DescriptorType.TYPE_ANNOTATION_TEXT ? null : reader.readUUID();

        if(!TYPE_DESCRIPTOR_MAP.containsKey(type)) {
            logger.error("Unknown type descriptor {}", type);
            throw new UnexpectedMessageException("Unsupported descriptor type " + type);
        }

        return new TypeDescriptorInfo<>(TYPE_DESCRIPTOR_MAP.get(type).apply(id, reader), type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable <T extends Enum<T>> Codec<?> buildCodec(
            TypeDescriptorInfo<T> descriptorInfo,
            Function<Integer, Codec<?>> getRelativeCodec,
            Function<Integer, TypeDescriptorInfo<?>> getRelativeDescriptor
    ) throws MissingCodecException {
        if(!(descriptorInfo.type instanceof DescriptorType)) {
            throw new IllegalArgumentException("Expected v1 descriptor type, got " + descriptorInfo.type.getClass().getName());
        }

        var metadata = descriptorInfo.descriptor instanceof MetadataDescriptor
                ? ((MetadataDescriptor)descriptorInfo.descriptor).getMetadata(getRelativeCodec, getRelativeDescriptor)
                : null;

        switch ((DescriptorType)descriptorInfo.type) {
            case ARRAY:
                var arrayDescriptor = descriptorInfo.as(ArrayTypeDescriptor.class);

                return CodecBuilder.getOrCreateCodec(
                        this,
                        descriptorInfo.getId(),
                        id -> new CompilableCodec(
                                id,
                                getRelativeCodec.apply(arrayDescriptor.type.intValue()),
                                ArrayCodec::new,
                                t -> Array.newInstance(t, 0).getClass()
                        )
                );
            case COMPOUND:
                var compoundDescriptor = descriptorInfo.as(CompoundTypeDescriptor.class);

                return

        }
    }
}
