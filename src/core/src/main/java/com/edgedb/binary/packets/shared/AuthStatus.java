package com.edgedb.binary.packets.shared;

import java.util.HashMap;
import java.util.Map;

public enum AuthStatus {
    AUTHENTICATION_OK                    (0),
    AUTHENTICATION_REQUIRED_SASL_MESSAGE (0xa),
    AUTHENTICATION_SASL_CONTINUE         (0xb),
    AUTHENTICATION_SASL_FINAL            (0xc);

    private final int value;
    private final static Map<Integer, AuthStatus> map = new HashMap<>();

    AuthStatus(int value) {
        this.value = value;
    }

    static {
        for (AuthStatus v : AuthStatus.values()) {
            map.put(v.value, v);
        }
    }

    public static AuthStatus valueOf(int raw) {
        return map.get(raw);
    }

    public int getValue() {
        return value;
    }
}
