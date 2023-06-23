package com.edgedb.driver.binary.packets.shared;

import com.edgedb.driver.binary.BinaryEnum;
import org.jetbrains.annotations.NotNull;

public enum AuthStatus implements BinaryEnum<Integer> {
    AUTHENTICATION_OK                    (0),
    AUTHENTICATION_REQUIRED_SASL_MESSAGE (0xa),
    AUTHENTICATION_SASL_CONTINUE         (0xb),
    AUTHENTICATION_SASL_FINAL            (0xc);

    private final int value;

    AuthStatus(int value) {
        this.value = value;
    }

    public @NotNull Integer getValue() {
        return value;
    }
}
