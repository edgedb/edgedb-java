package com.edgedb.binary.descriptors.common;

import com.edgedb.binary.BinaryEnum;
import org.joou.UInteger;

import java.util.HashMap;
import java.util.Map;

import static org.joou.Unsigned.uint;

public enum ShapeElementFlags implements BinaryEnum<UInteger> {
    NONE          (uint(0)),
    IMPLICIT      (uint(1)),
    LINK_PROPERTY (uint(1 << 1)),
    LINK          (uint(1 << 2));

    private final UInteger value;
    private final static Map<UInteger, ShapeElementFlags> map = new HashMap<>();
    ShapeElementFlags(UInteger value) {
        this.value = value;
    }
    static {
        for (ShapeElementFlags v : ShapeElementFlags.values()) {
            map.put(v.value, v);
        }
    }
    public static ShapeElementFlags valueOf(UInteger raw) {
        return map.get(raw);
    }
    @Override
    public UInteger getValue() {
        return this.value;
    }
}
