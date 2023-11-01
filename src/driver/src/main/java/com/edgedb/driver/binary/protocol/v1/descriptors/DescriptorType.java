package com.edgedb.driver.binary.protocol.v1.descriptors;

import com.edgedb.driver.binary.BinaryEnum;
import org.jetbrains.annotations.NotNull;

public enum DescriptorType implements BinaryEnum<Byte> {
    SET_DESCRIPTOR              (0x00),
    OBJECT_SHAPE_DESCRIPTOR     (0x01),
    BASE_SCALAR_TYPE_DESCRIPTOR (0x02),
    SCALAR_TYPE_DESCRIPTOR      (0x03),
    TUPLE_TYPE_DESCRIPTOR       (0x04),
    NAMED_TUPLE_DESCRIPTOR      (0x05),
    ARRAY_TYPE_DESCRIPTOR       (0x06),
    ENUMERATION_TYPE_DESCRIPTOR (0x07),
    INPUT_SHAPE_DESCRIPTOR      (0x08),
    RANGE_TYPE_DESCRIPTOR       (0x09),
    SCALAR_TYPE_NAME_ANNOTATION (0xff);

    private final byte value;

    DescriptorType(int value) {
        this.value = (byte)value;
    }

    @Override
    public @NotNull Byte getValue() {
        return this.value;
    }
}
