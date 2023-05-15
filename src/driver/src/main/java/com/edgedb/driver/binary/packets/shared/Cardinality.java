package com.edgedb.driver.binary.packets.shared;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.BinaryEnum;
import com.edgedb.driver.binary.SerializableData;
import com.edgedb.driver.util.BinaryProtocolUtils;

import javax.naming.OperationNotSupportedException;
import java.util.HashMap;
import java.util.Map;

public enum Cardinality implements SerializableData, BinaryEnum<Byte> {
    NO_RESULT    (0x6e),
    AT_MOST_ONE  (0x6f),
    ONE          (0x41),
    MANY         (0x6d),
    AT_LEAST_ONE (0x4d);


    private final byte value;
    private final static Map<Byte, Cardinality> map = new HashMap<>();

    Cardinality(int value) {
        this.value = (byte)value;
    }

    static {
        for (Cardinality v : Cardinality.values()) {
            map.put(v.value, v);
        }
    }

    public static Cardinality valueOf(byte raw) {
        return map.get(raw);
    }

    @Override
    public void write(PacketWriter writer) throws OperationNotSupportedException {
        writer.write(value);
    }

    @Override
    public int getSize() {
        return BinaryProtocolUtils.BYTE_SIZE;
    }

    @Override
    public Byte getValue() {
        return this.value;
    }
}
