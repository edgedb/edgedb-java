package com.edgedb.driver;

import com.edgedb.driver.abstractions.QueryDelegate;
import com.edgedb.driver.clients.TransactableClient;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.exceptions.TransactionException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class Transaction implements EdgeDBQueryable {
    private static final Logger logger = LoggerFactory.getLogger(Transaction.class);
    private final TransactableClient client;
    private final TransactionSettings settings;

    private final Semaphore semaphore;

    public Transaction(TransactableClient client, TransactionSettings settings) {
        this.client = client;
        this.settings = settings;
        this.semaphore = new Semaphore(1);
    }

    public <T> CompletionStage<T> run(Function<Transaction, CompletionStage<T>> func) {
        return start()
                .thenCompose((v) -> func.apply(this)
                        .thenApply((u) -> (CompletionStage<T>)CompletableFuture.completedFuture(u))
                        .exceptionally((e) -> {
                            logger.warn("Exception in transaction query", e);
                            return rollback().thenApply(t -> null);
                        }))
                .thenCompose(v -> v)
                .thenCompose(v -> commit().thenApply(u -> v).handle((u, e) ->  {
                    if(e != null) {
                        logger.error("Exception in transaction commit", e);
                    }

                    return u;
                }));
    }

    private CompletionStage<Void> start() {
        return this.client.startTransaction(settings.isolation, settings.isReadOnly, settings.isDeferrable);
    }

    private CompletionStage<Void> commit() {
        return this.client.commit();
    }

    private CompletionStage<Void> rollback() {
        return this.client.rollback();
    }


    private <T, U> CompletionStage<U> executeTransactionStep(
            Class<T> cls,
            String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities,
            AtomicInteger attempts,
            QueryDelegate<T, U> delegate
    ) {
        return CompletableFuture.completedFuture(null)
                .thenCompose((v) ->
                    delegate.run(cls, query, args, capabilities)
                        .thenApply(t -> (CompletionStage<U>)CompletableFuture.completedFuture(t))
                        .exceptionally(e -> {
                            if(e instanceof EdgeDBException) {
                                if(((EdgeDBException) e).shouldRetry) {

                                    if(attempts.getAndIncrement() <= settings.retryAttempts) {
                                        return executeTransactionStep(cls, query, args, capabilities, attempts, delegate);
                                    }
                                }
                            }

                            return CompletableFuture.failedFuture(new TransactionException("Transaction failed after" + settings.retryAttempts + "attempt(s)", e));
                        })
                ).thenCompose(v -> v);
    }

    private <T, U> CompletionStage<U> executeTransaction(
            Class<T> cls,
            String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities,
            QueryDelegate<T, U> delegate
    ) {
        final AtomicInteger attempts = new AtomicInteger();

        return CompletableFuture.runAsync(() -> {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
        }).thenCompose((v) -> executeTransactionStep(cls, query, args, capabilities, attempts, delegate));
    }

    @Override
    public CompletionStage<Void> execute(String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities) {
        return executeTransaction(Void.class, query, args, capabilities, (c, q, a, ca) -> client.execute(q, a, ca));
    }

    @Override
    public <T> CompletionStage<List<T>> query(Class<T> cls, String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities) {
        return executeTransaction(cls, query, args, capabilities, client::query);
    }

    @Override
    public <T> CompletionStage<T> querySingle(Class<T> cls, String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities) {
        return executeTransaction(cls, query, args, capabilities, client::querySingle);
    }

    @Override
    public <T> CompletionStage<T> queryRequiredSingle(Class<T> cls, String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities) {
        return executeTransaction(cls, query, args, capabilities, client::queryRequiredSingle);
    }

    public static class TransactionSettings {
        public static TransactionSettings getDefault() {
            return new TransactionSettings();
        }

        public TransactionIsolation isolation = TransactionIsolation.SERIALIZABLE;
        public boolean isReadOnly;
        public boolean isDeferrable;
        public int retryAttempts = 3;
    }
}
