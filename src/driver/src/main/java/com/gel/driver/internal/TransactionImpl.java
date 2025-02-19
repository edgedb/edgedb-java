package com.gel.driver.internal;

import com.gel.driver.Capabilities;
import com.gel.driver.Transaction;
import com.gel.driver.TransactionSettings;
import com.gel.driver.TransactionState;
import com.gel.driver.abstractions.QueryDelegate;
import com.gel.driver.clients.TransactableClient;
import com.gel.driver.datatypes.Json;
import com.gel.driver.exceptions.GelException;
import com.gel.driver.exceptions.TransactionException;
import org.jetbrains.annotations.NotNull;
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

public final class TransactionImpl implements Transaction {
    private static final Logger logger = LoggerFactory.getLogger(com.gel.driver.Transaction.class);
    private final TransactableClient client;
    private final TransactionSettings settings;

    private final @NotNull Semaphore semaphore;

    public TransactionImpl(TransactableClient client, TransactionSettings settings) {
        this.client = client;
        this.settings = settings;
        this.semaphore = new Semaphore(1);
    }

    public <T> CompletionStage<T> run(@NotNull Function<com.gel.driver.Transaction, CompletionStage<T>> func) {
        return start()
                .thenCompose((v) -> func.apply(this)
                        .thenApply((u) -> (CompletionStage<T>) CompletableFuture.completedFuture(u))
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
        return this.client.startTransaction(settings.getIsolation(), settings.isReadOnly(), settings.isDeferrable());
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
            @NotNull AtomicInteger attempts,
            @NotNull QueryDelegate<T, U> delegate
    ) {
        return CompletableFuture.completedFuture(null)
                .thenCompose((v) ->
                        delegate.run(cls, query, args, capabilities)
                                .thenApply(t -> (CompletionStage<U>)CompletableFuture.completedFuture(t))
                                .exceptionally(e -> {
                                    if(e instanceof GelException) {
                                        if(((GelException) e).shouldRetry) {

                                            if(attempts.getAndIncrement() <= settings.getRetryAttempts()) {
                                                return executeTransactionStep(cls, query, args, capabilities, attempts, delegate);
                                            }
                                        }
                                    }

                                    return CompletableFuture.failedFuture(new TransactionException("Transaction failed after" + settings.getRetryAttempts() + "attempt(s)", e));
                                })
                ).thenCompose(v -> v);
    }

    private <T, U> CompletionStage<U> executeTransaction(
            Class<T> cls,
            String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities,
            @NotNull QueryDelegate<T, U> delegate
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
    public TransactionState getState() {
        return client.getTransactionState();
    }

    @Override
    public CompletionStage<Void> execute(@NotNull String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities) {
        return executeTransaction(Void.class, query, args, capabilities, (c, q, a, ca) -> client.execute(q, a, ca));
    }

    @Override
    public <T> CompletionStage<List<T>> query(@NotNull Class<T> cls, @NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        return executeTransaction(cls, query, args, capabilities, client::query);
    }

    @Override
    public <T> CompletionStage<T> querySingle(@NotNull Class<T> cls, @NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        return executeTransaction(cls, query, args, capabilities, client::querySingle);
    }

    @Override
    public <T> CompletionStage<T> queryRequiredSingle(@NotNull Class<T> cls, @NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        return executeTransaction(cls, query, args, capabilities, client::queryRequiredSingle);
    }

    @Override
    public CompletionStage<Json> queryJson(@NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        return executeTransaction(Json.class, query, args, capabilities,
                (c, q, a, ca) -> client.queryJson(q, a, ca)
        );
    }

    @Override
    public CompletionStage<List<Json>> queryJsonElements(@NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        return executeTransaction(Json.class, query, args, capabilities,
                (c, q, a, ca) -> client.queryJsonElements(q, a, ca)
        );
    }
}
