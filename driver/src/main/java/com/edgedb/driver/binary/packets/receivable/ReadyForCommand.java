package com.edgedb.driver.binary.packets.receivable;

import com.edgedb.driver.TransactionState;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.shared.Annotation;

public class ReadyForCommand implements Receivable {
    public final Annotation[] annotations;
    public final TransactionState transactionState;

    public ReadyForCommand(PacketReader reader) {
        annotations = reader.readAnnotations();
        transactionState = reader.readEnum(TransactionState::valueOf, Byte.TYPE);
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.READY_FOR_COMMAND;
    }
}
