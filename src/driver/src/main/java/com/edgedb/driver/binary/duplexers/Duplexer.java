package com.edgedb.driver.binary.duplexers;

import com.edgedb.driver.binary.protocol.ProtocolProvider;
import com.edgedb.driver.binary.protocol.Receivable;
import com.edgedb.driver.binary.protocol.Sendable;
import com.edgedb.driver.exceptions.GelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Duplexer {
    public abstract ProtocolProvider getProtocolProvider();
    public abstract void reset();
    public abstract boolean isConnected();

    public abstract CompletionStage<Void> disconnect();
    public abstract CompletionStage<Receivable> readNext();
    public abstract CompletionStage<Void> send(Sendable packet, @Nullable Sendable... packets);
    public abstract CompletionStage<Void> duplex(
            DuplexCallback func,
            @NotNull Sendable packet,
            @Nullable Sendable... packets
    );

    public final CompletionStage<Void> send(Sendable packet) {
        return this.send(packet, (Sendable[]) null);
    }

    public final CompletionStage<Void> duplex(
            @NotNull Sendable packet,
            DuplexCallback func
    ) {
        return this.duplex(func, packet);
    }

    public final CompletionStage<Void> duplexAndSync(
            @NotNull Sendable packet,
            DuplexCallback func
    ) {
        return duplex(func, packet, getProtocolProvider().sync());
    }

    public final CompletionStage<Receivable> duplexSingle(Sendable packet) {
        return send(packet).thenCompose(v -> readNext());
    }

    @FunctionalInterface
    public interface DuplexCallback {
        CompletionStage<Void> process(DuplexResult result)
        throws GelException, OperationNotSupportedException;
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
        public <T>  void finishExceptionally(T p, @NotNull Function<T, Throwable> exceptionFactory) {
            promise.completeExceptionally(exceptionFactory.apply(p));
        }
        public <T, U>  void finishExceptionally(T p1, U p2, @NotNull BiFunction<T, U, Throwable> exceptionFactory) {
            promise.completeExceptionally(exceptionFactory.apply(p1, p2));
        }

    }
}
