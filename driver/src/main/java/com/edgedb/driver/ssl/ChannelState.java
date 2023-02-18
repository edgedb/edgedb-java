package com.edgedb.driver.ssl;

public enum ChannelState {
    NOOP,
    CONTINUE,
    HAS_APP_DATA,
    CLOSED,
    COMPLETE,
    CONTINUE_UNWRAP,
    CONTINUE_WRAP
}

