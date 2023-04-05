package com.edgedb.exceptions;

import com.edgedb.binary.packets.ServerMessageType;

@SuppressWarnings("ClassEscapesDefinedScope")
public class UnexpectedMessageException extends EdgeDBException {

    public UnexpectedMessageException(String message) {
        super(message);
    }

    public UnexpectedMessageException(ServerMessageType unexpected) {
        super(String.format("Got unexpected message type %s", unexpected));
    }

    public UnexpectedMessageException(ServerMessageType expected, ServerMessageType actual) {
        super(String.format("Expected message type %s but got %s", expected, actual));
    }
}
