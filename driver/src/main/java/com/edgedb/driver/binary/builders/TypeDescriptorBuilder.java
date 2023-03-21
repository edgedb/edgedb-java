package com.edgedb.driver.binary.builders;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.descriptors.*;
import com.edgedb.driver.exceptions.EdgeDBException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

import static org.joou.Unsigned.ubyte;

public final class TypeDescriptorBuilder {
    private static final Map<DescriptorType, BiFunction<UUID, PacketReader, ? extends TypeDescriptor>> typeDescriptorFactories;

    static {
        typeDescriptorFactories = new HashMap<>();
        typeDescriptorFactories.put(DescriptorType.ARRAY_TYPE_DESCRIPTOR,       ArrayTypeDescriptor::new);
        typeDescriptorFactories.put(DescriptorType.BASE_SCALAR_TYPE_DESCRIPTOR, BaseScalarTypeDescriptor::new);
        typeDescriptorFactories.put(DescriptorType.ENUMERATION_TYPE_DESCRIPTOR, EnumerationTypeDescriptor::new);
        typeDescriptorFactories.put(DescriptorType.NAMED_TUPLE_DESCRIPTOR,      NamedTupleTypeDescriptor::new);
        typeDescriptorFactories.put(DescriptorType.OBJECT_SHAPE_DESCRIPTOR,     ObjectShapeDescriptor::new);
        typeDescriptorFactories.put(DescriptorType.SCALAR_TYPE_DESCRIPTOR,      ScalarTypeDescriptor::new);
        typeDescriptorFactories.put(DescriptorType.SCALAR_TYPE_NAME_ANNOTATION, ScalarTypeNameAnnotation::new);
        typeDescriptorFactories.put(DescriptorType.SET_DESCRIPTOR,              SetTypeDescriptor::new);
        typeDescriptorFactories.put(DescriptorType.TUPLE_TYPE_DESCRIPTOR,       TupleTypeDescriptor::new);
        typeDescriptorFactories.put(DescriptorType.INPUT_SHAPE_DESCRIPTOR,      InputShapeDescriptor::new);
        typeDescriptorFactories.put(DescriptorType.RANGE_TYPE_DESCRIPTOR,       RangeTypeDescriptor::new);
    }

    public static TypeDescriptor getDescriptor(final PacketReader reader) throws EdgeDBException {
        var type = reader.readEnum(DescriptorType::valueOf, Byte.TYPE);
        var id = reader.readUUID();

        var factory = typeDescriptorFactories.get(type);

        if(factory != null) {
            return factory.apply(id, reader);
        }

        var v = ubyte(type.getValue());

        if (v.compareTo(ubyte(0x80)) >= 0 && v.compareTo(ubyte(0xfe)) <= 0) {
            return new TypeAnnotationDescriptor(type, id, reader);
        }

        throw new EdgeDBException(String.format("No descriptor found for type %X", v.byteValue()));
    }
}
