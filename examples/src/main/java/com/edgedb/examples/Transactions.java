package com.edgedb.examples;

import com.edgedb.driver.EdgeDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

public final class Transactions implements Example {
    private static final Logger logger = LoggerFactory.getLogger(Transactions.class);

    @Override
    public CompletionStage<Void> run(EdgeDBClient client) {
        return client.transaction(tx -> {
            logger.info("In transaction");
            return tx.queryRequiredSingle(String.class, "select 'Result from Transaction'");
        }).thenAccept(result -> {
            logger.info("Result from transaction: {}", result);
        });
    }
}
