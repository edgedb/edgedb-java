package com.edgedb.driver.util;

import io.netty.util.ReferenceCounted;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class ComposableUtil {
    public static <T, U extends CompletionStage<T>> CompletableFuture<T> exceptionallyCompose(@NotNull CompletionStage<T> a, @NotNull Function<Throwable, U> f) {
        return CompletableFuture.completedFuture(null)
                .thenCompose(v -> a
                        .thenApply(CompletableFuture::completedFuture)
                        .exceptionally(e -> f.apply(e).toCompletableFuture())
                )
                .thenCompose(v -> v);
    }

    public static <T, U extends CompletionStage<T>, V extends AutoCloseable> CompletionStage<T> composeWith(
            @NotNull V with,
            @NotNull Function<V, U> composed
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

    public static <T, U extends CompletionStage<T>, V extends ReferenceCounted> CompletionStage<T> composeWith(
            @NotNull V with,
            @NotNull Function<V, U> composed
    ) {
        return composed.apply(with)
                .thenApply((v) -> {
                            try {
                                with.release();
                            } catch (Exception x) {
                                throw new CompletionException(x);
                            }

                            return v;
                        }
                );
    }

    public static <T, U extends CompletionStage<T>, V extends AutoCloseable, W extends CompletionStage<V>>
    CompletionStage<T> composeWith(
            @NotNull W with,
            @NotNull Function<V, U> composed
    ) {
        return with.thenCompose(r -> composeWith(r, composed));
    }
}
