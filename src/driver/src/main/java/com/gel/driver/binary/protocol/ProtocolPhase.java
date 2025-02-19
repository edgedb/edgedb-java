package com.gel.driver.binary.protocol;

public enum ProtocolPhase {
    CONNECTION,
    AUTH,
    COMMAND,
    DUMP,
    TERMINATION,
    ERRORED;
}
