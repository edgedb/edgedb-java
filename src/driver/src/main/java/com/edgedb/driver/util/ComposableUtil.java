package com.edgedb.driver.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class ComposableUtil {
    public static <T, U extends CompletionStage<T>> CompletableFuture<T> exceptionallyCompose(CompletionStage<T> a, Function<Throwable, U> f) {
        return CompletableFuture.completedFuture(null)
                .thenCompose(v -> a
                        .thenApply(CompletableFuture::completedFuture)
                        .exceptionally(e -> f.apply(e).toCompletableFuture())
                )
                .thenCompose(v -> v);
    }

    public static <T, U extends CompletionStage<T>, V extends AutoCloseable> CompletionStage<T> composeWith(
            V with,
            Function<V, U> composed
    ) {
        return composed.apply(with)
                .thenApply((v) -> {
                    try {
                        with.close();
                    } catch (Exception x) {
                        throw new CompletionException(x);
                    }

                    return v;
                }
        );
    }

    public static <T, U extends CompletionStage<T>, V extends AutoCloseable, W extends CompletionStage<V>>
    CompletionStage<T> composeWith(
            W with,
            Function<V, U> composed
    ) {
        return with.thenCompose(r -> composeWith(r, composed));
    }
}
