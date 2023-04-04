package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.driver.Transaction;
import com.edgedb.driver.TransactionIsolation;
import com.edgedb.driver.TransactionState;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface TransactableClient extends EdgeDBQueryable {
    TransactionState getTransactionState();

    default <T> CompletionStage<T> transaction(Function<Transaction, CompletionStage<T>> func) {
        return transaction(Transaction.TransactionSettings.getDefault(), func);
    }

    default <T> CompletionStage<T> transaction(
            Transaction.TransactionSettings settings,
            Function<Transaction, CompletionStage<T>> func
    ) {
        var tx = new Transaction(this, settings);
        return tx.run(func);
    }

    CompletionStage<Void> startTransaction(TransactionIsolation isolation, boolean readonly, boolean deferrable);

    CompletionStage<Void> commit();

    CompletionStage<Void> rollback();
}
