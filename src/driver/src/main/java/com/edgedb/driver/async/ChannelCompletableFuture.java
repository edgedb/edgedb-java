package com.edgedb.driver.async;

import io.netty.channel.ChannelFuture;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ChannelCompletableFuture extends CompletableFuture<Void> {
    public static @NotNull ChannelCompletableFuture completeFrom(@NotNull ChannelFuture future) {
        var completableFuture = new ChannelCompletableFuture();

        future.addListener((v) -> {
            if(v.cause() != null) {
                completableFuture.completeExceptionally(v.cause());
                return;
            }

            if(v.isCancelled()) {
                completableFuture.cancel(true);
                return;
            }

            completableFuture.complete(null);
        });

        return completableFuture;
    }

    public CompletableFuture<Void> thenCompose(@NotNull ChannelFuture future) {
        return this.thenCompose((v) -> ChannelCompletableFuture.completeFrom(future));
    }
}
