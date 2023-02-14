package com.edgedb.driver.binary.duplexers;

import com.edgedb.driver.binary.packets.receivables.Receiable;
import com.edgedb.driver.binary.packets.sendables.Sendable;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.ssl.SSLAsynchronousSocketChannel;

import java.util.concurrent.CompletableFuture;

public class ChannelDuplexer extends Duplexer {
    private final EdgeDBBinaryClient client;
    private SSLAsynchronousSocketChannel channel;

    public ChannelDuplexer(EdgeDBBinaryClient client) {
        this.client = client;
    }

    @Override
    public void reset() {

    }

    public void init(SSLAsynchronousSocketChannel channel) {
        this.channel = channel;
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
    public CompletableFuture<Void> sendAsync(Sendable packet) {
        return null;
    }
}
