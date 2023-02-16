package com.edgedb.driver.binary.duplexers;

import com.edgedb.driver.binary.packets.receivable.Receivable;
import com.edgedb.driver.binary.packets.sendables.Sendable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public abstract class Duplexer {
    private boolean isConnected;

    public void setIsConnected(boolean value) {
        isConnected = value;
    }

    public boolean getIsConnected() {
        return isConnected;
    }

    public abstract void reset();

    public abstract CompletionStage<Void> disconnectAsync();
    public abstract CompletionStage<Receivable> readNextAsync();
    public abstract CompletionStage<Void> sendAsync(Sendable packet, @Nullable Sendable... packets) throws SSLException;
    public abstract CompletionStage<Void> duplexAsync(Function<DuplexResult, CompletionStage<Void>> func, @NotNull Sendable packet, @Nullable Sendable... packets) throws SSLException;

    public final CompletionStage<Void> sendAsync(Sendable packet) throws SSLException {
        return this.sendAsync(packet, (Sendable[]) null);
    }

    public final CompletionStage<Void> duplexAsync(Sendable packet, Function<DuplexResult, CompletionStage<Void>> func) throws SSLException {
        return this.duplexAsync(func, packet);
    }

    public static class DuplexResult {
        public final Receivable packet;

        private final CompletableFuture<Void> promise;

        public DuplexResult(Receivable packet, CompletableFuture<Void> promise) {
            this.packet = packet;
            this.promise = promise;
        }


        public void finishDuplexing() {
            promise.complete(null);
        }

        public void finishExceptionally(Throwable err) {
            promise.completeExceptionally(err);
        }
    }
}
