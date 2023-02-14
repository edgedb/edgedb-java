package com.edgedb.driver.binary.packets;

import com.edgedb.driver.binary.PacketWriter;

import javax.naming.OperationNotSupportedException;

public interface SerializableData {
    void write(final PacketWriter writer) throws OperationNotSupportedException;
    int getSize();

}
