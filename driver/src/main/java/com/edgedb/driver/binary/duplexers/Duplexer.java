package com.edgedb.driver.binary.duplexers;

import com.edgedb.driver.binary.packets.receivables.Receiable;
import com.edgedb.driver.binary.packets.sendables.Sendable;

import java.util.concurrent.CompletableFuture;

public abstract class Duplexer {
    private boolean isConnected;

    public void setIsConnected(boolean value) {
        isConnected = value;
    }

    public boolean getIsConnected() {
        return isConnected;
    }

    public abstract void reset();

    public abstract CompletableFuture<Void> disconnectAsync();
    public abstract CompletableFuture<Receiable> readNextAsync();
    public abstract CompletableFuture<Void> sendAsync(Sendable packet);

}
