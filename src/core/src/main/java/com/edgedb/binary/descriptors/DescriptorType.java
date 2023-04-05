package com.edgedb.binary.descriptors;

import com.edgedb.binary.BinaryEnum;

import java.util.HashMap;
import java.util.Map;

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
    private final static Map<Byte, DescriptorType> map = new HashMap<>();
    DescriptorType(int value) {
        this.value = (byte)value;
    }

    static {
        for (DescriptorType v : DescriptorType.values()) {
            map.put(v.value, v);
        }
    }
    public static DescriptorType valueOf(byte raw) {
        return map.get(raw);
    }

    @Override
    public Byte getValue() {
        return this.value;
    }
}
