package com.gel.driver.binary.protocol.common.descriptors;

import com.gel.driver.binary.BinaryEnum;
import org.joou.UInteger;

import static org.joou.Unsigned.uint;

public enum ShapeElementFlags implements BinaryEnum<UInteger> {
    NONE          (uint(0)),
    IMPLICIT      (uint(1)),
    LINK_PROPERTY (uint(1 << 1)),
    LINK          (uint(1 << 2));

    private final UInteger value;

    ShapeElementFlags(UInteger value) {
        this.value = value;
    }

    @Override
    public UInteger getValue() {
        return this.value;
    }
}
