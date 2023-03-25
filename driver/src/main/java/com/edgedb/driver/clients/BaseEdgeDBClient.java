package com.edgedb.driver.clients;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBClientConfig;
import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.state.Session;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public abstract class BaseEdgeDBClient {
    private boolean isConnected = false;
    private final EdgeDBConnection connection;
    private final EdgeDBClientConfig config;

    // TODO: remove when 'clients' are no longer exposed
    @SuppressWarnings("ClassEscapesDefinedScope")
    protected Session session;

    public BaseEdgeDBClient(EdgeDBConnection connection, EdgeDBClientConfig config) {
        this.connection = connection;
        this.config = config;
        this.session = new Session();
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
    public EdgeDBClientConfig getConfig() {
        return this.config;
    }


    public abstract CompletionStage<Void> executeAsync(String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities);
    public CompletionStage<Void> executeAsync(String query) {
        return executeAsync(query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }
    public CompletionStage<Void> executeAsync(String query, EnumSet<Capabilities> capabilities){
        return executeAsync(query, null, capabilities);
    }

    public abstract <T> CompletionStage<List<T>> queryAsync(String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities, Class<T> cls);
    public <T> CompletionStage<List<T>> queryAsync(String query, Class<T> cls) {
        return queryAsync(query, null, EnumSet.of(Capabilities.MODIFICATIONS), cls);
    }
    public <T> CompletionStage<List<T>> queryAsync(String query, EnumSet<Capabilities> capabilities, Class<T> cls) {
        return queryAsync(query, null, capabilities, cls);
    }

    public abstract <T> CompletionStage<T> querySingleAsync(String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities, Class<T> cls);
    public <T> CompletionStage<T> querySingleAsync(String query, Class<T> cls) {
        return querySingleAsync(query, null, EnumSet.of(Capabilities.MODIFICATIONS), cls);
    }

    public <T> CompletionStage<T> querySingleAsync(String query, EnumSet<Capabilities> capabilities, Class<T> cls) {
        return querySingleAsync(query, null, capabilities, cls);
    }

    public abstract <T> CompletionStage<T> queryRequiredSingleAsync(String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities, Class<T> cls);
    public <T> CompletionStage<T> queryRequiredSingleAsync(String query, Class<T> cls) {
        return queryRequiredSingleAsync(query, null, EnumSet.of(Capabilities.MODIFICATIONS), cls);
    }

    public <T> CompletionStage<T> queryRequiredSingleAsync(String query, EnumSet<Capabilities> capabilities, Class<T> cls) {
        return queryRequiredSingleAsync(query, null, capabilities, cls);
    }

    public abstract CompletionStage<Void> connectAsync();
    public abstract CompletionStage<Void> disconnectAsync();

    public CompletionStage<Void> reconnectAsync() {
        return disconnectAsync().thenCompose((v) -> connectAsync());
    }
}
