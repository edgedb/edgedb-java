package com.gel.driver;

import com.gel.driver.binary.BinaryEnum;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the state of a {@linkplain Transaction}.
 */
public enum TransactionState implements BinaryEnum<Byte> {

    /**
     * The client isn't in a transaction.
     */
    NOT_IN_TRANSACTION (0x49),

    /**
     * The client is in a transaction.
     */
    IN_TRANSACTION (0x54),

    /**
     * The client is in a failed transaction.
     */
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

    /**
     * Gets a transaction state from a raw byte.
     * @param raw The raw value of the transaction state.
     * @return The transaction state represented by the byte if there is a predefined state; otherwise <code>null</code>.
     */
    public static TransactionState valueOf(byte raw) {
        return map.get(raw);
    }

    @Override
    public @NotNull Byte getValue() {
        return this.value;
    }
}
