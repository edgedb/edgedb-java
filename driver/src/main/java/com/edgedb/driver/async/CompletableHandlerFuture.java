package com.edgedb.driver.async;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

public class CompletableHandlerFuture<T, U> extends CompletableFuture<T> implements CompletionHandler<T,U> {
    @Override
    public void completed(T result, U attachment) {
        this.complete(result);
    }

    @Override
    public void failed(Throwable exc, U attachment) {
        this.completeExceptionally(exc);
    }
}
