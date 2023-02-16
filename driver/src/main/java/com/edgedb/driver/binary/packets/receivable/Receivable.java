package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.binary.packets.ServerMessageType;

public interface Receivable {
    ServerMessageType getMessageType();
}
