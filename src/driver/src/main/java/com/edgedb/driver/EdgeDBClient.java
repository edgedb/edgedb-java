package com.edgedb.driver;

import com.edgedb.driver.abstractions.ClientQueryDelegate;
import com.edgedb.driver.clients.BaseEdgeDBClient;
import com.edgedb.driver.clients.EdgeDBTCPClient;
import com.edgedb.driver.clients.StatefulClient;
import com.edgedb.driver.clients.TransactableClient;
import com.edgedb.driver.datatypes.Json;
import com.edgedb.driver.exceptions.ConfigurationException;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.state.Config;
import com.edgedb.driver.state.Session;
import com.edgedb.driver.util.ClientPoolHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a client pool used to interact with EdgeDB.
 */
public final class EdgeDBClient implements StatefulClient, EdgeDBQueryable {
    private static final Logger logger = LoggerFactory.getLogger(EdgeDBClient.class);

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

    /**
     * Constructs a new {@linkplain EdgeDBClient}.
     * @param connection The connection parameters used to connect this client to EdgeDB.
     * @param config The configuration for this client.
     * @throws ConfigurationException A configuration parameter is invalid.
     */
    public EdgeDBClient(EdgeDBConnection connection, EdgeDBClientConfig config) throws ConfigurationException {
        this.clients = new ConcurrentLinkedQueue<>();
        this.config = config;
        this.connection = connection;
        this.poolHolder = new ClientPoolHolder(config.getPoolSize());
        this.clientFactory = createClientFactory();
        this.session = Session.DEFAULT;
        this.clientAvailability = config.getClientAvailability();
    }

    private ClientFactory createClientFactory() throws ConfigurationException {
        if(config.getClientType() == ClientType.TCP) {
            return EdgeDBTCPClient::new;
        }

        throw new ConfigurationException(String.format("No such implementation for client type %s found", this.config.getClientType()));
    }

    /**
     * Constructs a new {@linkplain EdgeDBClient}.
     * @param connection The connection parameters used to connect this client to EdgeDB.
     * @throws ConfigurationException A configuration parameter is invalid.
     */
    public EdgeDBClient(EdgeDBConnection connection) throws EdgeDBException {
        this(connection, EdgeDBClientConfig.DEFAULT);
    }

    /**
     * Constructs a new {@linkplain EdgeDBClient}.
     * @param config The configuration for this client.
     * @throws IOException The connection arguments couldn't be automatically resolved.
     * @throws ConfigurationException A configuration parameter is invalid.
     */
    public EdgeDBClient(EdgeDBClientConfig config) throws IOException, ConfigurationException {
        this(EdgeDBConnection.resolveEdgeDBTOML(), config);
    }

    /**
     * Constructs a new {@linkplain EdgeDBClient}.
     * @throws IOException The connection arguments couldn't be automatically resolved.
     * @throws ConfigurationException A configuration parameter is invalid.
     */
    public EdgeDBClient() throws IOException, EdgeDBException {
        this(EdgeDBConnection.resolveEdgeDBTOML(), EdgeDBClientConfig.DEFAULT);
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

    /**
     * Gets whether this client supports transactions.
     * @return {@code true} if the client supports transactions; otherwise {@code false}.
     */
    public boolean supportsTransactions() {
        return this.config.getClientType() == ClientType.TCP;
    }

    /**
     * Initializes a transaction and executes the callback with the transaction object.
     * @param settings The transaction settings to use.
     * @param func The callback to execute with the transaction.
     * @return A {@linkplain CompletionStage} that represents the asynchronous operation of creating and executing the
     * transaction. The result of the {@linkplain CompletionStage} is the result of the supplied callback.
     * @param <T> The result of the query.
     */
    public <T> CompletionStage<T> transaction(
            TransactionSettings settings,
            Function<Transaction, CompletionStage<T>> func
    ) {
        return getTransactableClient().thenCompose(client -> client.transaction(settings, func));
    }

    /**
     * Initializes a transaction and executes the callback with the transaction object.
     * @param func The callback to execute with the transaction.
     * @return A {@linkplain CompletionStage} that represents the asynchronous operation of creating and executing the
     * transaction. The result of the {@linkplain CompletionStage} is the result of the supplied callback.
     * @param <T> The result of the query.
     */
    public <T> CompletionStage<T> transaction(Function<Transaction, CompletionStage<T>> func) {
        return getTransactableClient().thenCompose(client -> client.transaction(func));
    }

    /**
     * Creates a new client instance with the specified {@linkplain Session}.
     * <br/><br/>
     * The returned client shares the same underlying client pool as this client.
     * @param session The session for the new client.
     * @return A new client instance with the applied session, sharing the same underlying client pool.
     */
    @Override
    public EdgeDBClient withSession(@NotNull Session session) {
        return new EdgeDBClient(this, session);
    }

    /**
     * Creates a new client instance with the specified module aliases.
     * <br/><br/>
     * The returned client shares the same underlying client pool as this client.
     * @param aliases The module aliases for the new client.
     * @return A new client instance with the applied module aliases, sharing the same underlying client pool.
     */
    @Override
    public EdgeDBClient withModuleAliases(@NotNull Map<String, String> aliases) {
        return new EdgeDBClient(this, this.session.withModuleAliases(aliases));
    }

    /**
     * Creates a new client instance with the specified config.
     * <br/><br/>
     * The returned client shares the same underlying client pool as this client.
     * @param config The config for the new client.
     * @return A new client instance with the applied module aliases, sharing the same underlying client pool.
     */
    @Override
    public EdgeDBClient withConfig(@NotNull Config config) {
        return new EdgeDBClient(this, this.session.withConfig(config));
    }

    /**
     * Creates a new client instance with the specified config builder.
     * <br/><br/>
     * The returned client shares the same underlying client pool as this client.
     * @param func A delegate that populates the config builder.
     * @return A new client instance with the applied module aliases, sharing the same underlying client pool.
     */
    @Override
    public EdgeDBClient withConfig(@NotNull Consumer<Config.Builder> func) {
        return new EdgeDBClient(this, this.session.withConfig(func));
    }

    /**
     * Creates a new client instance with the specified globals.
     * <br/><br/>
     * The returned client shares the same underlying client pool as this client.
     * @param globals The globals for the new client.
     * @return A new client instance with the applied module aliases, sharing the same underlying client pool.
     */
    @Override
    public EdgeDBClient withGlobals(@NotNull Map<String, Object> globals) {
        return new EdgeDBClient(this, this.session.withGlobals(globals));
    }

    /**
     * Creates a new client instance with the specified module
     * <br/><br/>
     * The returned client shares the same underlying client pool as this client.
     * @param module The module for the new client.
     * @return A new client instance with the applied module aliases, sharing the same underlying client pool.
     */
    @Override
    public EdgeDBClient withModule(@NotNull String module) {
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
    public CompletionStage<Void> execute(@NotNull String query, @Nullable Map<String, Object> args, EnumSet<Capabilities> capabilities) {
        return executePooledQuery(Void.class, query, args, capabilities,
                (c, cls, q, a, ca) -> c.execute(q,a,ca)
        );
    }

    @Override
    public <T> CompletionStage<List<T>> query(@NotNull Class<T> cls, @NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        return executePooledQuery(cls, query, args, capabilities, EdgeDBQueryable::query);
    }

    @Override
    public <T> CompletionStage<T> querySingle(@NotNull Class<T> cls, @NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        return executePooledQuery(cls, query, args, capabilities, EdgeDBQueryable::querySingle);
    }

    @Override
    public <T> CompletionStage<T> queryRequiredSingle(@NotNull Class<T> cls, @NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        return executePooledQuery(cls, query, args, capabilities, EdgeDBQueryable::queryRequiredSingle);
    }

    @Override
    public CompletionStage<Json> queryJson(@NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        return executePooledQuery(Json.class, query, args, capabilities,
                (c, cls, q, a, ca) -> c.queryJson(q, a, ca)
        );
    }

    @Override
    public CompletionStage<List<Json>> queryJsonElements(@NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        return executePooledQuery(Json.class, query, args, capabilities,
                (c, cls, q, a, ca) -> c.queryJsonElements(q, a, ca)
        );
    }

    private synchronized CompletionStage<BaseEdgeDBClient> getClient() {
        logger.trace("polling cached clients...");
        var cachedClient = clients.poll();

        if(cachedClient != null) {
            logger.debug(
                    "returning cached client, cached client count: {}; age {}",
                    this.clientCount.decrementAndGet(),
                    cachedClient.age()
            );

            cachedClient.touch();
            return CompletableFuture.completedFuture(cachedClient.client);
        }

        return createClient();
    }

    private synchronized CompletionStage<TransactableClient> getTransactableClient() {
        return getClient()
                .thenApply(client -> {
                    if(!(client instanceof TransactableClient)) {
                        logger.warn(
                                "A request for a client that supports transactions cannot be fulfilled, the client" +
                                        " provided from the pool is of type {} which doesn't support transactions.",
                                client.getClass().getSimpleName()
                        );
                        throw new CompletionException(
                                new EdgeDBException("Cannot use transactions with " + client + " type")
                        );
                    }

                    return (TransactableClient) client;
                });
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

        logger.debug("client {} returned to pool, client count: {}", client, count);

        if(count > this.clientAvailability) {
            logger.debug("Cleaning up pool... {}/{} availability reached", count, this.clientAvailability);
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
                    logger.trace("Contract acquired, remaining handles: {}", this.poolHolder.remaining());
                    var client = clientFactory.create(this.connection, this.config, contract);
                    contract.register(client, this::acceptClient);
                    client.onReady(this::onClientReady);
                    logger.debug("client instance created: {}", client);
                    return client;
                })
                .thenApply(client -> client.withSession(this.session));
    }

    @FunctionalInterface
    private interface ClientFactory {
        BaseEdgeDBClient create(EdgeDBConnection connection, EdgeDBClientConfig config, AutoCloseable poolHandle);
    }
}