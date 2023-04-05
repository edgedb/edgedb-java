package com.edgedb.binary.packets.receivable;

import com.edgedb.binary.packets.shared.Annotation;
import com.edgedb.TransactionState;
import com.edgedb.binary.PacketReader;
import com.edgedb.binary.packets.ServerMessageType;

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
