package com.edgedb;

import com.edgedb.binary.BinaryEnum;

import java.util.HashMap;
import java.util.Map;

public enum ErrorSeverity implements BinaryEnum<Byte> {
    ERROR (0x78),
    FATAL (0xC8),
    PANIC (0xFF);

    private final byte value;
    private final static Map<Byte, ErrorSeverity> map = new HashMap<>();
    ErrorSeverity(int value) {
        this.value = (byte)value;
    }

    static {
        for (ErrorSeverity v : ErrorSeverity.values()) {
            map.put(v.value, v);
        }
    }

    public static ErrorSeverity valueOf(byte raw) {
        return map.get(raw);
    }

    @Override
    public Byte getValue() {
        return value;
    }
}
