package com.edgedb.driver;

import com.edgedb.driver.binary.BinaryEnum;

import java.util.HashMap;
import java.util.Map;

public enum TransactionState implements BinaryEnum<Byte> {
    NOT_IN_TRANSACTION (0x49),
    IN_TRANSACTION (0x54),
    IN_FAILED_TRANSACTION (0x45);

    private final byte value;
    private final static Map<Byte, TransactionState> map = new HashMap<>();
    TransactionState(int value) {
        this.value = (byte)value;
    }

    static {
        for (TransactionState v : TransactionState.values()) {
            map.put(v.value, v);
        }
    }

    public static TransactionState valueOf(byte raw) {
        return map.get(raw);
    }

    @Override
    public Byte getValue() {
        return this.value;
    }
}
