package com.edgedb.examples;

import com.edgedb.driver.EdgeDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

public final class BasicQueryFunctions implements Example {
    private static final Logger logger = LoggerFactory.getLogger(BasicQueryFunctions.class);

    @Override
    public CompletionStage<Void> run(EdgeDBClient client) {
        return client.query(String.class, "select 'Hello, Java!'")
                .thenAccept(result -> logger.info("query result: {}", result))
                .thenCompose(v -> client.querySingle(String.class, "select 'Hello, Java!'"))
                .thenAccept(result -> logger.info("querySingle result: {}", result))
                .thenCompose(v -> client.queryRequiredSingle(String.class, "select 'Hello, Java!'"))
                .thenAccept(result -> logger.info("queryRequiredSingle result: {}", result));
    }
}
