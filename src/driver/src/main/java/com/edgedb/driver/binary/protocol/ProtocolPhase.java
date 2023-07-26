package com.edgedb.driver.binary.protocol;

public enum ProtocolPhase {
    CONNECTION,
    AUTH,
    COMMAND,
    DUMP,
    TERMINATION,
    ERRORED;
}
