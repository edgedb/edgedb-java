package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBConnection;

import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CompletionStage;

public abstract class BaseEdgeDBClient {
    private boolean isConnected = false;
    private final EdgeDBConnection connection;

    public BaseEdgeDBClient(EdgeDBConnection connection) {
        this.connection = connection;
    }

    void setIsConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }
    public boolean getIsConnected() {
        return isConnected;
    }

    public EdgeDBConnection getConnection() {
        return this.connection;
    }

    public abstract CompletionStage<Void> executeAsync(String query, Hashtable<String, Object> args);
    public CompletionStage<Void> executeAsync(String query){
        return executeAsync(query, null);
    }

    public abstract <T> CompletionStage<List<T>> queryAsync(String query, Hashtable<String, Object> args);
    public <T> CompletionStage<List<T>> queryAsync(String query) {
        return queryAsync(query, null);
    }

    public abstract <T> CompletionStage<T> querySingleAsync(String query, Hashtable<String, Object> args);
    public <T> CompletionStage<T> querySingleAsync(String query) {
        return querySingleAsync(query, null);
    }

    public abstract <T> CompletionStage<T> queryRequiredSingleAsync(String query, Hashtable<String, Object> args);
    public <T> CompletionStage<T> queryRequiredSingleAsync(String query) {
        return queryRequiredSingleAsync(query, null);
    }
}
