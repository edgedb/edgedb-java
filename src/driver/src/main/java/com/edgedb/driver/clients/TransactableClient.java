package com.edgedb.driver.clients;

import com.edgedb.driver.*;
import com.edgedb.driver.internal.TransactionImpl;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface TransactableClient extends EdgeDBQueryable {
    TransactionState getTransactionState();

    default <T> CompletionStage<T> transaction(Function<Transaction, CompletionStage<T>> func) {
        return transaction(TransactionSettings.DEFAULT, func);
    }

    default <T> CompletionStage<T> transaction(
            TransactionSettings settings,
            Function<Transaction, CompletionStage<T>> func
    ) {
        var tx = new TransactionImpl(this, settings);
        return tx.run(func);
    }

    CompletionStage<Void> startTransaction(TransactionIsolation isolation, boolean readonly, boolean deferrable);

    CompletionStage<Void> commit();

    CompletionStage<Void> rollback();
}
