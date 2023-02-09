package com.edgedb.driver.binary.duplexers;

import com.edgedb.driver.binary.packets.receivables.Receiable;
import com.edgedb.driver.binary.packets.sendables.Sendable;
import com.edgedb.driver.clients.EdgeDBBinaryClient;

import java.util.concurrent.CompletableFuture;

public class StreamDuplexer extends Duplexer {
    private final EdgeDBBinaryClient client;

    public StreamDuplexer(EdgeDBBinaryClient client) {
        this.client = client;
    }

    @Override
    public void reset() {

    }

    @Override
    public CompletableFuture<Void> disconnectAsync() {
        return null;
    }

    @Override
    public CompletableFuture<Receiable> readNextAsync() {
        return null;
    }

    @Override
    CompletableFuture<Void> sendAsync(Sendable packet) {
        return null;
    }
}
