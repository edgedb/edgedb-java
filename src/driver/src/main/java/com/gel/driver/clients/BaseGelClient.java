package com.gel.driver.clients;

import com.gel.driver.GelClientConfig;
import com.gel.driver.GelConnection;
import com.gel.driver.GelQueryable;
import com.gel.driver.async.AsyncEvent;
import com.gel.driver.state.Config;
import com.gel.driver.state.Session;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class BaseGelClient implements StatefulClient, GelQueryable, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(BaseGelClient.class);
    private final @NotNull AsyncEvent<BaseGelClient> onReady;
    private final GelConnection connection;
    private final GelClientConfig config;
    private final AutoCloseable poolHandle;

    protected Session session;

    public BaseGelClient(GelConnection connection, GelClientConfig config, AutoCloseable poolHandle) {
        this.connection = connection;
        this.config = config;
        this.session = new Session();
        this.poolHandle = poolHandle;
        this.onReady = new AsyncEvent<>();
    }

    public void onReady(Function<BaseGelClient, CompletionStage<?>> handler) {
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
    public GelClientConfig getConfig() {
        return this.config;
    }

    @Override
    public @NotNull BaseGelClient withSession(@NotNull Session session) {
        this.session = session;
        return this;
    }

    @Override
    public @NotNull BaseGelClient withModuleAliases(@NotNull Map<String, String> aliases) {
        this.session = this.session.withModuleAliases(aliases);
        return this;
    }

    @Override
    public @NotNull BaseGelClient withConfig(@NotNull Config config) {
        this.session = this.session.withConfig(config);
        return this;
    }

    @Override
    public @NotNull BaseGelClient withConfig(@NotNull Consumer<Config.Builder> func) {
        this.session = this.session.withConfig(func);
        return this;
    }

    @Override
    public @NotNull BaseGelClient withGlobals(@NotNull Map<String, Object> globals) {
        this.session = this.session.withGlobals(globals);
        return this;
    }

    @Override
    public @NotNull BaseGelClient withModule(@NotNull String module) {
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
