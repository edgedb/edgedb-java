package com.edgedb.examples;

import com.edgedb.driver.GelClientPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class Transactions implements Example {
    private static final Logger logger = LoggerFactory.getLogger(Transactions.class);

    @Override
    public CompletionStage<Void> run(GelClientPool clientPool) {
        // verify we can run transactions
        if(!clientPool.supportsTransactions()) {
            logger.info("Skipping transactions, client type {} doesn't support it", clientPool.getClientType());
            return CompletableFuture.completedFuture(null);
        }

        return clientPool.transaction(tx -> {
            logger.info("In transaction");
            return tx.queryRequiredSingle(String.class, "select 'Result from Transaction'");
        }).thenAccept(result -> {
            logger.info("Result from transaction: {}", result);
        });
    }
}