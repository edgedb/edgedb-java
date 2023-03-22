package com.edgedb.driver.binary.codecs.common;

import com.edgedb.driver.binary.BinaryEnum;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.SerializableData;

import javax.naming.OperationNotSupportedException;
import java.util.HashMap;
import java.util.Map;

import static com.edgedb.driver.util.BinaryProtocolUtils.BYTE_SIZE;

public enum RangeFlags implements SerializableData, BinaryEnum<Byte> {
    EMPTY (1),
    INCLUDE_LOWER_BOUNDS (1 << 1),
    INCLUDE_UPPER_BOUNDS (1 << 2),
    INFINITE_LOWER_BOUNDS (1 << 3),
    INFINITE_UPPER_BOUNDS (1 << 4);
    private final byte value;
    private final static Map<Byte, RangeFlags> map = new HashMap<>();

    RangeFlags(int value) {
        this.value = (byte)value;
    }

    static {
        for (RangeFlags v : RangeFlags.values()) {
            map.put(v.value, v);
        }
    }
    public static RangeFlags valueOf(byte raw) {
        return map.get(raw);
    }

    @Override
    public Byte getValue() {
        return this.value;
    }

    @Override
    public void write(PacketWriter writer) throws OperationNotSupportedException {
        writer.write(this.value);
    }

    @Override
    public int getSize() {
        return BYTE_SIZE;
    }
}
