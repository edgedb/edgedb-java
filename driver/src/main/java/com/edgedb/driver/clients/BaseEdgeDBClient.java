package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBClientConfig;
import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.async.AsyncEvent;
import com.edgedb.driver.state.Config;
import com.edgedb.driver.state.Session;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public abstract class BaseEdgeDBClient implements StatefulClient, AutoCloseable {
    private final AsyncEvent<BaseEdgeDBClient> onReady;
    private boolean isConnected = false;
    private final EdgeDBConnection connection;
    private final EdgeDBClientConfig config;
    private final AutoCloseable poolHandle;

    // TODO: remove when 'clients' are no longer exposed
    protected Session session;

    public BaseEdgeDBClient(EdgeDBConnection connection, EdgeDBClientConfig config, AutoCloseable poolHandle) {
        this.connection = connection;
        this.config = config;
        this.session = new Session();
        this.poolHandle = poolHandle;
        this.onReady = new AsyncEvent<>();
    }

    public void onReady(Function<BaseEdgeDBClient, CompletionStage<?>> handler) {
        this.onReady.add(handler);
    }

    protected CompletionStage<Void> dispatchReady() {
        return this.onReady.dispatch(this);
    }

    public abstract Map<String, Object> getServerConfig();

    public abstract Optional<Long> getSuggestedPoolConcurrency();

    void setIsConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }
    public boolean isConnected() {
        return isConnected;
    }

    public EdgeDBConnection getConnection() {
        return this.connection;
    }
    public EdgeDBClientConfig getConfig() {
        return this.config;
    }

    @Override
    public BaseEdgeDBClient withSession(Session session) {
        this.session = session;
        return this;
    }

    @Override
    public BaseEdgeDBClient withModuleAliases(Map<String, String> aliases) {
        this.session = this.session.withModuleAliases(aliases);
        return this;
    }

    @Override
    public BaseEdgeDBClient withConfig(Config config) {
        this.session = this.session.withConfig(config);
        return this;
    }

    @Override
    public BaseEdgeDBClient withGlobals(Map<String, Object> globals) {
        this.session = this.session.withGlobals(globals);
        return this;
    }

    @Override
    public BaseEdgeDBClient withModule(String module) {
        this.session = this.session.withModule(module);
        return this;
    }

    public abstract CompletionStage<Void> connect();
    public abstract CompletionStage<Void> disconnect();

    public CompletionStage<Void> reconnect() {
        return disconnect().thenCompose((v) -> connect());
    }

    @Override
    public void close() throws Exception {
        this.poolHandle.close();
    }
}
