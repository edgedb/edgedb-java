package com.edgedb.driver.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class ExceptionallyCompose {
    public static <T, U extends CompletionStage<T>> CompletableFuture<T> exceptionallyCompose(CompletionStage<T> a, Function<Throwable, U> f) {
        return CompletableFuture.completedFuture(null)
                .thenCompose(v -> a
                        .thenApply(CompletableFuture::completedFuture)
                        .exceptionally(e -> f.apply(e).toCompletableFuture())
                )
                .thenCompose(v -> v);
    }
}
