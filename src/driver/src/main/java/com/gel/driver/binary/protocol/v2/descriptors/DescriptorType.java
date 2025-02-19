package com.gel.driver.binary.protocol.v2.descriptors;

import com.gel.driver.binary.BinaryEnum;

public enum DescriptorType implements BinaryEnum<Byte> {
    SET(0),
    OBJECT_OUTPUT(1),
    SCALAR(3),
    TUPLE(4),
    NAMED_TUPLE(5),
    ARRAY(6),
    ENUMERATION(7),
    INPUT(8),
    RANGE(9),
    OBJECT(10),
    COMPOUND(11),
    MULTI_RANGE(12),
    TYPE_ANNOTATION_TEXT(127);

    private final byte value;

    DescriptorType(int value) {
        this.value = (byte)(value & 0xFF);
    }

    @Override
    public Byte getValue() {
        return this.value;
    }
}
