package com.edgedb.driver.binary.protocol;

public enum ClientMessageType {
    AUTHENTICATION_SASL_INITIAL_RESPONSE (0x70),
    AUTHENTICATION_SASL_RESPONSE         (0x72),
    CLIENT_HANDSHAKE                     (0x56),
    DUMP                                 (0x3e),
    PARSE                                (0x50),

    EXECUTE                              (0x4f),
    RESTORE                              (0x3c),
    RESTORE_BLOCK                        (0x3d),
    RESTORE_EOF                          (0x2e),
    SYNC                                 (0x53),
    TERMINATE                            (0x58);

    private final byte code;

    ClientMessageType(int code) {
        this.code = (byte)code;
    }

    public byte getCode() {
        return code;
    }
}
