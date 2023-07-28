package com.edgedb.driver.binary.protocol.v2;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.builders.CodecBuilder;
import com.edgedb.driver.binary.codecs.*;
import com.edgedb.driver.binary.protocol.ProtocolProvider;
import com.edgedb.driver.binary.protocol.ProtocolVersion;
import com.edgedb.driver.binary.protocol.TypeDescriptor;
import com.edgedb.driver.binary.protocol.TypeDescriptorInfo;
import com.edgedb.driver.binary.protocol.v1.V1ProtocolProvider;
import com.edgedb.driver.binary.protocol.v2.descriptors.*;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.datatypes.Range;
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
            put(DescriptorType.SET, SetTypeDescriptor::new);
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
        var length = reader.readUInt32();

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
                        metadata,
                        (id, meta) -> new CompilableCodec(
                                id,
                                meta,
                                getRelativeCodec.apply(arrayDescriptor.type.intValue()),
                                ArrayCodec::new,
                                t -> Array.newInstance(t, 0).getClass()
                        )
                );
            case COMPOUND:
            {
                var compoundDescriptor = descriptorInfo.as(CompoundTypeDescriptor.class);

                var innerCodecs = new Codec<?>[compoundDescriptor.components.length];

                for(var i = 0; i != compoundDescriptor.components.length; i++) {
                    innerCodecs[i] = getRelativeCodec.apply(compoundDescriptor.components[i].intValue());
                }


                return CodecBuilder.getOrCreateCodec(
                        this,
                        descriptorInfo.getId(),
                        metadata,
                        (id, meta) -> new CompoundCodec(
                                id,
                                meta,
                                compoundDescriptor.operation,
                                innerCodecs
                        )
                );
            }
            case ENUMERATION:
                var enumerationDescriptor = descriptorInfo.as(EnumerationTypeDescriptor.class);

                return CodecBuilder.getOrCreateCodec(
                        this,
                        descriptorInfo.getId(),
                        metadata,
                        (id, meta) -> new EnumerationCodec(
                                id,
                                meta,
                                enumerationDescriptor.members
                        )
                );
            case INPUT:
                var inputDescriptor = descriptorInfo.as(InputShapeDescriptor.class);

                var names = new String[inputDescriptor.elements.length];
                var codecs = new Codec<?>[inputDescriptor.elements.length];

                for(var i = 0; i != inputDescriptor.elements.length; i++) {
                    var element = inputDescriptor.elements[i];
                    names[i] = element.name;
                    codecs[i] = getRelativeCodec.apply(element.typePosition.intValue());
                }

                return CodecBuilder.getOrCreateCodec(
                        this,
                        descriptorInfo.getId(),
                        metadata,
                        (id, meta) -> new SparseObjectCodec(
                                id,
                                meta,
                                codecs,
                                names
                        )
                );
            case NAMED_TUPLE:
            {
                var namedTupleDescriptor = descriptorInfo.as(NamedTupleTypeDescriptor.class);

                var elements = new ObjectCodec.ObjectProperty[namedTupleDescriptor.elements.length];

                for(var i = 0; i != namedTupleDescriptor.elements.length; i++) {
                    var element = namedTupleDescriptor.elements[i];
                    elements[i] = new ObjectCodec.ObjectProperty(
                            element.name,
                            getRelativeCodec.apply((int)element.typePosition),
                            null
                    );
                }

                return CodecBuilder.getOrCreateCodec(
                        this,
                        descriptorInfo.getId(),
                        metadata,
                        (id, meta) -> new ObjectCodec(
                                id,
                                null,
                                meta,
                                elements
                        )
                );
            }
            case OBJECT:
                return null;
            case OBJECT_OUTPUT:
            {
                var objectOutputDescriptor = descriptorInfo.as(ObjectOutputShapeDescriptor.class);
                ObjectTypeDescriptor objectDescriptor = null;

                if(!objectOutputDescriptor.isEphemeralFreeShape) {
                    objectDescriptor= getRelativeDescriptor
                            .apply(objectOutputDescriptor.type.intValue())
                            .as(ObjectTypeDescriptor.class);

                    metadata = objectDescriptor.getMetadata(getRelativeCodec, getRelativeDescriptor);
                }

                final var typeId = objectDescriptor == null
                        ? null
                        : objectDescriptor.id;

                var elements = new ObjectCodec.ObjectProperty[objectOutputDescriptor.elements.length];

                for(var i = 0; i != objectOutputDescriptor.elements.length; i++) {
                    var element = objectOutputDescriptor.elements[i];

                    elements[i] = new ObjectCodec.ObjectProperty(
                            element.name,
                            getRelativeCodec.apply(element.typePosition.intValue()),
                            element.cardinality
                    );
                }

                return CodecBuilder.getOrCreateCodec(
                        this,
                        descriptorInfo.getId(), // use the shapes ID for cache control.
                        metadata,
                        (i, meta) -> new ObjectCodec(
                                i,
                                typeId,
                                meta,
                                elements
                        )
                );
            }
            case RANGE:
                var rangeDescriptor = descriptorInfo.as(RangeTypeDescriptor.class);

                return CodecBuilder.getOrCreateCodec(
                        this,
                        descriptorInfo.getId(),
                        metadata,
                        (id, meta) ->
                                new CompilableCodec(
                                        id,
                                        meta,
                                        getRelativeCodec.apply(rangeDescriptor.type.intValue()),
                                        RangeCodec::new,
                                        t -> Range.empty(t).getClass()
                                )
                );
            case SCALAR:
                throw new MissingCodecException(
                        "Could not find the scalar type " + descriptorInfo.getId().toString()
                                + ". Please file a bug report with your query that caused this error"
                );
            case SET:
                var setDescriptor = descriptorInfo.as(SetTypeDescriptor.class);

                return CodecBuilder.getOrCreateCodec(
                        this,
                        descriptorInfo.getId(),
                        metadata,
                        (id, meta) ->
                                new CompilableCodec(
                                        id,
                                        meta,
                                        getRelativeCodec.apply(setDescriptor.type.intValue()),
                                        SetCodec::new,
                                        t -> Array.newInstance(t, 0).getClass()
                                )
                );
            case TUPLE:
            {
                var tupleDescriptor = descriptorInfo.as(TupleTypeDescriptor.class);

                var innerCodecs = new Codec<?>[tupleDescriptor.elements.length];

                for(int i = 0; i != innerCodecs.length; i++) {
                    innerCodecs[i] = getRelativeCodec.apply(tupleDescriptor.elements[i].intValue());
                }

                return CodecBuilder.getOrCreateCodec(
                        this,
                        descriptorInfo.getId(),
                        metadata,
                        (id, meta) -> new TupleCodec(
                                id,
                                meta,
                                innerCodecs
                        )
                );
            }
            default:
                throw new MissingCodecException(
                        "Could not find a type descriptor with the type "
                        + ((DescriptorType) descriptorInfo.type).getValue()
                        + ". Please file a bug report with your query that caused this error."
                );
        }
    }
}
