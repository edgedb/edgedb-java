package com.edgedb.driver.async;

import io.netty.channel.ChannelFuture;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class ChannelCompletableFuture extends CompletableFuture<Void> {
    private static final Logger logger = LoggerFactory.getLogger(ChannelCompletableFuture.class);

    public static @NotNull ChannelCompletableFuture completeFrom(@NotNull ChannelFuture future) {
        var completableFuture = new ChannelCompletableFuture();
        logger.debug("Registering {}", future.hashCode());

        future.addListener((v) -> {
            if(v.cause() != null) {
                logger.debug("Failing from {}", future.hashCode());
                completableFuture.completeExceptionally(v.cause());
                return;
            }

            if(v.isCancelled()) {
                logger.debug("Cancelling from {}", future.hashCode());
                completableFuture.cancel(true);
                return;
            }

            logger.debug("Completing from {}", future.hashCode());

            completableFuture.complete(null);
        });

        return completableFuture;
    }

    public CompletableFuture<Void> thenCompose(@NotNull ChannelFuture future) {
        return this.thenCompose((v) -> ChannelCompletableFuture.completeFrom(future));
    }
}
