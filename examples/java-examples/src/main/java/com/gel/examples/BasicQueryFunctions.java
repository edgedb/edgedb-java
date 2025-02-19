package com.gel.examples;

import com.gel.driver.GelClientPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

public final class BasicQueryFunctions implements Example {
    private static final Logger logger = LoggerFactory.getLogger(BasicQueryFunctions.class);

    @Override
    public CompletionStage<Void> run(GelClientPool clientPool) {
        return clientPool.query(String.class, "select 'Hello, Java!'")
                .thenAccept(result -> logger.info("query result: {}", result))
                .thenCompose(v -> clientPool.querySingle(String.class, "select 'Hello, Java!'"))
                .thenAccept(result -> logger.info("querySingle result: {}", result))
                .thenCompose(v -> clientPool.queryRequiredSingle(String.class, "select 'Hello, Java!'"))
                .thenAccept(result -> logger.info("queryRequiredSingle result: {}", result));
    }
}