package com.edgedb.driver.binary.builders;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.descriptors.*;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

import static org.joou.Unsigned.ubyte;

public final class TypeDescriptorBuilder {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final @NotNull Map<DescriptorType, BiFunction<UUID, PacketReader, ? extends TypeDescriptor>> typeDescriptorFactories;

    static {
        typeDescriptorFactories = new HashMap<>() {
            {
                put(DescriptorType.ARRAY_TYPE_DESCRIPTOR,       ArrayTypeDescriptor::new);
                put(DescriptorType.BASE_SCALAR_TYPE_DESCRIPTOR, BaseScalarTypeDescriptor::new);
                put(DescriptorType.ENUMERATION_TYPE_DESCRIPTOR, EnumerationTypeDescriptor::new);
                put(DescriptorType.NAMED_TUPLE_DESCRIPTOR,      NamedTupleTypeDescriptor::new);
                put(DescriptorType.OBJECT_SHAPE_DESCRIPTOR,     ObjectShapeDescriptor::new);
                put(DescriptorType.SCALAR_TYPE_DESCRIPTOR,      ScalarTypeDescriptor::new);
                put(DescriptorType.SCALAR_TYPE_NAME_ANNOTATION, ScalarTypeNameAnnotation::new);
                put(DescriptorType.SET_DESCRIPTOR,              SetTypeDescriptor::new);
                put(DescriptorType.TUPLE_TYPE_DESCRIPTOR,       TupleTypeDescriptor::new);
                put(DescriptorType.INPUT_SHAPE_DESCRIPTOR,      InputShapeDescriptor::new);
                put(DescriptorType.RANGE_TYPE_DESCRIPTOR,       RangeTypeDescriptor::new);
            }
        };
    }

    public static @NotNull TypeDescriptorResult getDescriptor(final @NotNull PacketReader reader) throws EdgeDBException {
        var type = reader.readEnum(DescriptorType.class, Byte.TYPE);
        var id = reader.readUUID();

        var factory = typeDescriptorFactories.get(type);

        if(factory != null) {
            return new TypeDescriptorResult(type, factory.apply(id, reader));
        }

        var v = ubyte(type.getValue());

        if (v.compareTo(ubyte(0x80)) >= 0 && v.compareTo(ubyte(0xfe)) <= 0) {
            return new TypeDescriptorResult(type, new TypeAnnotationDescriptor(type, id, reader));
        }

        throw new EdgeDBException(String.format("No descriptor found for type %X", v.byteValue()));
    }

    public static class TypeDescriptorResult {
        public final DescriptorType type;
        private final TypeDescriptor descriptor;

        public TypeDescriptorResult(DescriptorType type, TypeDescriptor descriptor) {
            this.type = type;
            this.descriptor = descriptor;
        }

        public UUID getId() {
            return descriptor.getId();
        }

        @SuppressWarnings("unchecked")
        public <T extends TypeDescriptor> T as(Class<T> ignored) {
            return (T)descriptor;
        }
    }
}
