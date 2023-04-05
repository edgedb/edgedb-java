package com.edgedb.async;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

public class CompletableHandlerFuture<T, U> extends CompletableFuture<CompletableHandlerFuture<T,U>.HandlerState> implements CompletionHandler<T,U> {
    @Override
    public void completed(T result, U attachment) {
        this.complete(new HandlerState(result, attachment));
    }

    @Override
    public void failed(Throwable exc, U attachment) {
        this.completeExceptionally(exc);
    }

    public CompletableFuture<T> flatten() {
        return this.thenApply((state) -> state.result);
    }

    public class HandlerState {
        public final T result;
        public final U attachment;

        public HandlerState(T result, U attachment) {
            this.result = result;
            this.attachment = attachment;
        }
    }
}
