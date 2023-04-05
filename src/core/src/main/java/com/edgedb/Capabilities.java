package com.edgedb;

import com.edgedb.binary.BinaryEnum;
import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.SerializableData;
import com.edgedb.util.BinaryProtocolUtils;

import javax.naming.OperationNotSupportedException;
import java.util.HashMap;
import java.util.Map;

public enum Capabilities implements SerializableData, BinaryEnum<Long> {
    READ_ONLY         (0),
    MODIFICATIONS     (1),
    SESSION_CONFIG    (1 << 1),
    TRANSACTION       (1 << 2),
    DDL               (1 << 3),
    PERSISTENT_CONFIG(1 << 4),
    ALL               (0xffffffffffffffffL);

    private final long value;
    private final static Map<Long, Capabilities> map = new HashMap<>();
    Capabilities(long value) {
        this.value = value;
    }
    static {
        for (Capabilities v : Capabilities.values()) {
            map.put(v.value, v);
        }
    }
    public static Capabilities valueOf(long raw) {
        return map.get(raw);
    }

    @SuppressWarnings("ClassEscapesDefinedScope")
    @Override
    public void write(PacketWriter writer) throws OperationNotSupportedException {
        writer.write(value);
    }

    @Override
    public int getSize() {
        return BinaryProtocolUtils.LONG_SIZE;
    }

    @Override
    public Long getValue() {
        return this.value;
    }
}
