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


    public abstract CompletionStage<Void> execute(
            String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities
    );
    public CompletionStage<Void> execute(String query) {
        return execute(query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }
    public CompletionStage<Void> execute(String query, EnumSet<Capabilities> capabilities){
        return execute(query, null, capabilities);
    }

    public abstract <T> CompletionStage<List<T>> query(
            String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities,
            Class<T> cls
    );
    public <T> CompletionStage<List<T>> query(String query, Class<T> cls) {
        return query(query, null, EnumSet.of(Capabilities.MODIFICATIONS), cls);
    }
    public <T> CompletionStage<List<T>> query(String query, EnumSet<Capabilities> capabilities, Class<T> cls) {
        return query(query, null, capabilities, cls);
    }

    public abstract <T> CompletionStage<T> querySingle(
            String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities,
            Class<T> cls
    );
    public <T> CompletionStage<T> querySingle(String query, Class<T> cls) {
        return querySingle(query, null, EnumSet.of(Capabilities.MODIFICATIONS), cls);
    }

    public <T> CompletionStage<T> querySingle(String query, EnumSet<Capabilities> capabilities, Class<T> cls) {
        return querySingle(query, null, capabilities, cls);
    }

    public abstract <T> CompletionStage<T> queryRequiredSingle(
            String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities,
            Class<T> cls
    );
    public <T> CompletionStage<T> queryRequiredSingle(String query, Class<T> cls) {
        return queryRequiredSingle(query, null, EnumSet.of(Capabilities.MODIFICATIONS), cls);
    }

    public <T> CompletionStage<T> queryRequiredSingle(String query, EnumSet<Capabilities> capabilities, Class<T> cls) {
        return queryRequiredSingle(query, null, capabilities, cls);
    }

    public abstract CompletionStage<Void> connect();
    public abstract CompletionStage<Void> disconnect();

    public CompletionStage<Void> reconnect() {
        return disconnect().thenCompose((v) -> connect());
    }
}
