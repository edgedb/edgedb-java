package com.edgedb.driver.binary.packets;

import java.util.HashMap;
import java.util.Map;

public enum ServerMessageType {
    AUTHENTICATION (0x52),
    COMMAND_COMPLETE (0x43),
    COMMAND_DATA_DESCRIPTION (0x54),
    STATE_DATA_DESCRIPTION (0x73),
    DATA (0x44),
    DUMP_HEADER (0x40),
    DUMP_BLOCK (0x3d),
    ERROR_RESPONSE (0x45),
    LOG_MESSAGE (0x4c),
    PARAMETER_STATUS (0x53),
    READY_FOR_COMMAND (0x5a),
    RESTORE_READY (0x2b),
    SERVER_HANDSHAKE (0x76),
    SERVER_KEY_DATA (0x4b);

    private final byte code;
    private final static Map<Byte, ServerMessageType> map = new HashMap<>();

    ServerMessageType(int code) {
        this.code = (byte)code;
    }

    static {
        for (ServerMessageType v : ServerMessageType.values()) {
            map.put(v.code, v);
        }
    }

    public static ServerMessageType valueOf(Byte raw) {
        return map.get(raw);
    }

    public byte getCode() {
        return code;
    }
}
