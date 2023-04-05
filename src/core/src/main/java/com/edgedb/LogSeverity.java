package com.edgedb;

import com.edgedb.binary.BinaryEnum;

import java.util.HashMap;
import java.util.Map;

public enum LogSeverity implements BinaryEnum<Byte> {
    DEBUG   (0x14),
    INFO    (0x28),
    NOTICE  (0x3C),
    WARNING (0x50);

    private final byte value;
    private final static Map<Byte, LogSeverity> map = new HashMap<>();
    LogSeverity(int value){
        this.value = (byte)value;
    }

    static {
        for (LogSeverity v : LogSeverity.values()) {
            map.put(v.value, v);
        }
    }

    public static LogSeverity valueOf(byte raw) {
        return map.get(raw);
    }

    @Override
    public Byte getValue() {
        return this.value;
    }
}
