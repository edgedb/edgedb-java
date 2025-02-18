package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBClientConfig;
import com.edgedb.driver.GelConnection;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.driver.async.AsyncEvent;
import com.edgedb.driver.state.Config;
import com.edgedb.driver.state.Session;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class BaseEdgeDBClient implements StatefulClient, EdgeDBQueryable, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(BaseEdgeDBClient.class);
    private final @NotNull AsyncEvent<BaseEdgeDBClient> onReady;
    private final GelConnection connection;
    private final EdgeDBClientConfig config;
    private final AutoCloseable poolHandle;

    protected Session session;

    public BaseEdgeDBClient(GelConnection connection, EdgeDBClientConfig config, AutoCloseable poolHandle) {
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

    public abstract Optional<Long> getSuggestedPoolConcurrency();

    public abstract boolean isConnected();

    public GelConnection getConnectionArguments() {
        return this.connection;
    }
    public EdgeDBClientConfig getConfig() {
        return this.config;
    }

    @Override
    public @NotNull BaseEdgeDBClient withSession(@NotNull Session session) {
        this.session = session;
        return this;
    }

    @Override
    public @NotNull BaseEdgeDBClient withModuleAliases(@NotNull Map<String, String> aliases) {
        this.session = this.session.withModuleAliases(aliases);
        return this;
    }

    @Override
    public @NotNull BaseEdgeDBClient withConfig(@NotNull Config config) {
        this.session = this.session.withConfig(config);
        return this;
    }

    @Override
    public @NotNull BaseEdgeDBClient withConfig(@NotNull Consumer<Config.Builder> func) {
        this.session = this.session.withConfig(func);
        return this;
    }

    @Override
    public @NotNull BaseEdgeDBClient withGlobals(@NotNull Map<String, Object> globals) {
        this.session = this.session.withGlobals(globals);
        return this;
    }

    @Override
    public @NotNull BaseEdgeDBClient withModule(@NotNull String module) {
        this.session = this.session.withModule(module);
        return this;
    }

    public abstract CompletionStage<Void> connect();
    public abstract CompletionStage<Void> disconnect();

    public CompletionStage<Void> reconnect() {
        return disconnect().thenCompose((v) -> {
            logger.debug("Executing connection attempt from reconnect");
            return connect();
        });
    }

    @Override
    public void close() throws Exception {
        this.poolHandle.close();
    }
}
