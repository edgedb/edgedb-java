package com.gel.driver.binary.protocol.v1.receivables;

import com.gel.driver.binary.protocol.Receivable;
import com.gel.driver.binary.protocol.common.Annotation;
import com.gel.driver.TransactionState;
import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.ServerMessageType;
import org.jetbrains.annotations.NotNull;

public class ReadyForCommand implements Receivable {
    public final Annotation @NotNull [] annotations;
    public final TransactionState transactionState;

    public ReadyForCommand(@NotNull PacketReader reader) {
        annotations = reader.readAnnotations();
        transactionState = reader.readEnum(TransactionState.class, Byte.TYPE);
    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.READY_FOR_COMMAND;
    }
}
