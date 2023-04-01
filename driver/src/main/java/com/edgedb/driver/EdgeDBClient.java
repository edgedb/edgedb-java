package com.edgedb.driver;

import com.edgedb.driver.abstractions.ClientQueryDelegate;
import com.edgedb.driver.clients.BaseEdgeDBClient;
import com.edgedb.driver.clients.EdgeDBTCPClient;
import com.edgedb.driver.clients.StatefulClient;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.state.Config;
import com.edgedb.driver.state.Session;
import com.edgedb.driver.util.ClientPoolHolder;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class EdgeDBClient implements StatefulClient {
    private static final class PooledClient {
        public final BaseEdgeDBClient client;
        public Instant lastUsed;

        public PooledClient(BaseEdgeDBClient client) {
            this.client = client;
            lastUsed = Instant.now();
        }

        public void touch() {
            lastUsed = Instant.now();
        }

        public Duration age() {
            return Duration.of(ChronoUnit.MICROS.between(lastUsed, Instant.now()), ChronoUnit.MICROS);
        }
    }

    private final AtomicInteger clientCount = new AtomicInteger();
    private final ConcurrentLinkedQueue<PooledClient> clients;
    private final EdgeDBConnection connection;
    private final EdgeDBClientConfig config;
    private final ClientPoolHolder poolHolder;
    private final ClientFactory clientFactory;
    private final Session session;
    private final int clientAvailability;

    public EdgeDBClient(EdgeDBConnection connection, EdgeDBClientConfig config) throws EdgeDBException {
        this.clients = new ConcurrentLinkedQueue<>();
        this.config = config;
        this.connection = connection;
        this.poolHolder = new ClientPoolHolder(config.getPoolSize());
        this.clientFactory = createClientFactory();
        this.session = Session.DEFAULT;
        this.clientAvailability = config.getClientAvailability();
    }

    private ClientFactory createClientFactory() throws EdgeDBException {
        if(config.getClientType() == ClientType.TCP) {
            return EdgeDBTCPClient::new;
        }

        throw new EdgeDBException(String.format("No such implementation for client type %s found", this.config.getClientType()));
    }

    public EdgeDBClient(EdgeDBConnection connection) throws EdgeDBException {
        this(connection, EdgeDBClientConfig.getDefault());
    }

    public EdgeDBClient(EdgeDBClientConfig config) throws IOException, EdgeDBException {
        this(EdgeDBConnection.resolveEdgeDBTOML(), config);
    }

    public EdgeDBClient() throws IOException, EdgeDBException {
        this(EdgeDBConnection.resolveEdgeDBTOML(), EdgeDBClientConfig.getDefault());
    }

    private EdgeDBClient(EdgeDBClient other, Session session) {
        this.clients = new ConcurrentLinkedQueue<>();
        this.config = other.config;
        this.connection = other.connection;
        this.poolHolder = other.poolHolder;
        this.clientFactory = other.clientFactory;
        this.session = session;
        this.clientAvailability = other.clientAvailability;
    }

    @Override
    public EdgeDBClient withSession(Session session) {
        return new EdgeDBClient(this, session);
    }

    @Override
    public EdgeDBClient withModuleAliases(Map<String, String> aliases) {
        return new EdgeDBClient(this, this.session.withModuleAliases(aliases));
    }

    @Override
    public EdgeDBClient withConfig(Config config) {
        return new EdgeDBClient(this, this.session.withConfig(config));
    }

    @Override
    public EdgeDBClient withGlobals(Map<String, Object> globals) {
        return new EdgeDBClient(this, this.session.withGlobals(globals));
    }

    @Override
    public EdgeDBClient withModule(String module) {
        return new EdgeDBClient(this, this.session.withModule(module));
    }

    private static final class ExecutePair<U> {
        public final BaseEdgeDBClient client;
        public final U result;

        private ExecutePair(BaseEdgeDBClient client, U result) {
            this.client = client;
            this.result = result;
        }
    }

    private <T, U> CompletionStage<U> executePooledQuery(
            Class<T> cls, String query, Map<String, Object> args,
            EnumSet<Capabilities> capabilities, ClientQueryDelegate<T, U> delegate
    ) {
        return getClient()
                .thenCompose(client ->
                    delegate.run(
                        client,
                        cls,
                        query,
                        args,
                        capabilities
                    ).thenApply(r -> new ExecutePair<>(client, r))
                )
                .thenApply(pair -> {
                    try {
                        pair.client.close();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }

                    return pair.result;
                });
    }

    @Override
    public CompletionStage<Void> execute(String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities) {
        return executePooledQuery(Void.class, query, args, capabilities,
                (c, cls, q, a, ca) -> c.execute(q,a,ca)
        );
    }

    @Override
    public <T> CompletionStage<List<T>> query(Class<T> cls, String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities) {
        return executePooledQuery(cls, query, args, capabilities, EdgeDBQueryable::query);
    }

    @Override
    public <T> CompletionStage<T> querySingle(Class<T> cls, String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities) {
        return executePooledQuery(cls, query, args, capabilities, EdgeDBQueryable::querySingle);
    }

    @Override
    public <T> CompletionStage<T> queryRequiredSingle(Class<T> cls, String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities) {
        return executePooledQuery(cls, query, args, capabilities, EdgeDBQueryable::queryRequiredSingle);
    }

    private synchronized CompletionStage<BaseEdgeDBClient> getClient() {
        var client = clients.poll();

        if(client != null) {
            this.clientCount.decrementAndGet();
            client.touch();
            return CompletableFuture.completedFuture(client.client);
        }

        return createClient();
    }

    private void cleanupPool() {
        clients.removeIf(c ->
                c.age().compareTo(this.config.getClientMaxAge()) > 0
                || (!c.client.isConnected() && this.clientCount.decrementAndGet() >= this.clientAvailability)
        );
    }

    private synchronized void acceptClient(BaseEdgeDBClient client) {
        this.clients.add(new PooledClient(client));
        var count = this.clientCount.incrementAndGet();

        if(count > this.clientAvailability) {
            cleanupPool();
        }
    }

    private synchronized CompletionStage<Void> onClientReady(BaseEdgeDBClient client) {
        var suggestedConcurrency = client.getSuggestedPoolConcurrency();

        suggestedConcurrency.ifPresent(this.poolHolder::resize);

        return CompletableFuture.completedFuture(null);
    }

    private CompletionStage<BaseEdgeDBClient> createClient() {
        return this.poolHolder.acquireContract()
                .thenApply(contract -> {
                    var client = clientFactory.create(this.connection, this.config, contract);
                    contract.register(client, this::acceptClient);
                    client.onReady(this::onClientReady);
                    return client;
                })
                .thenApply(client -> client.withSession(this.session));
    }

    @FunctionalInterface
    private interface ClientFactory {
        BaseEdgeDBClient create(EdgeDBConnection connection, EdgeDBClientConfig config, AutoCloseable poolHandle);
    }
}
