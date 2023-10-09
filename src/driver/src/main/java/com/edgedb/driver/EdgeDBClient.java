package com.edgedb.driver;

import com.edgedb.driver.abstractions.ClientQueryDelegate;
import com.edgedb.driver.clients.*;
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

import static com.edgedb.driver.util.ComposableUtil.composeWith;

/**
 * Represents a client pool used to interact with EdgeDB.
 */
public final class EdgeDBClient implements StatefulClient, EdgeDBQueryable, AutoCloseable {
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
    private final @NotNull ConcurrentLinkedQueue<PooledClient> clients;
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
    public EdgeDBClient(EdgeDBConnection connection, @NotNull EdgeDBClientConfig config) throws ConfigurationException {
        this.clients = new ConcurrentLinkedQueue<>();
        this.config = config;
        this.connection = connection;
        this.poolHolder = new ClientPoolHolder(config.getPoolSize());
        this.clientFactory = createClientFactory();
        this.session = Session.DEFAULT;
        this.clientAvailability = config.getClientAvailability();
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
    public EdgeDBClient(@NotNull EdgeDBClientConfig config) throws IOException, ConfigurationException {
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

    private EdgeDBClient(@NotNull EdgeDBClient other, Session session) {
        this.clients = new ConcurrentLinkedQueue<>();
        this.config = other.config;
        this.connection = other.connection;
        this.poolHolder = other.poolHolder;
        this.clientFactory = other.clientFactory;
        this.session = session;
        this.clientAvailability = other.clientAvailability;
    }

    public int getClientCount() {
        return this.clientCount.get();
    }

    private @NotNull ClientFactory createClientFactory() throws ConfigurationException {
        if(config.getClientType() == ClientType.TCP) {
            return EdgeDBTCPClient::new;
        } else if (config.getClientType() == ClientType.HTTP) {
            return EdgeDBHttpClient::new;
        }

        throw new ConfigurationException(String.format("No such implementation for client type %s found", this.config.getClientType()));
    }

    /**
     * Gets the underlying client type for this client pool.
     * @return The underlying client type, usually based on transport.
     * @see ClientType
     */
    public ClientType getClientType() {
        return config.getClientType();
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
            @NotNull Function<Transaction, CompletionStage<T>> func
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
    public <T> CompletionStage<T> transaction(@NotNull Function<Transaction, CompletionStage<T>> func) {
        return composeWith(getTransactableClient(), client -> client.transaction(func));
    }

    /**
     * Creates a new client instance with the specified {@linkplain Session}.
     * <br/><br/>
     * The returned client shares the same underlying client pool as this client.
     * @param session The session for the new client.
     * @return A new client instance with the applied session, sharing the same underlying client pool.
     */
    @Override
    public @NotNull EdgeDBClient withSession(@NotNull Session session) {
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
    public @NotNull EdgeDBClient withModuleAliases(@NotNull Map<String, String> aliases) {
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
    public @NotNull EdgeDBClient withConfig(@NotNull Config config) {
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
    public @NotNull EdgeDBClient withConfig(@NotNull Consumer<Config.Builder> func) {
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
    public @NotNull EdgeDBClient withGlobals(@NotNull Map<String, Object> globals) {
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
    public @NotNull EdgeDBClient withModule(@NotNull String module) {
        return new EdgeDBClient(this, this.session.withModule(module));
    }

    // added because Map.entry cannot contain nulls
    private static final class ExecutePair<U> {
        private final BaseEdgeDBClient client;
        private final @Nullable U result;

        private ExecutePair(BaseEdgeDBClient client, @Nullable U result) {
            this.client = client;
            this.result = result;
        }

        public @Nullable U getResult() {
            return result;
        }

        public BaseEdgeDBClient getClient() {
            return client;
        }
    }

    private <T, U> CompletionStage<U> executePooledQuery(
            Class<T> cls, String query, Map<String, Object> args,
            EnumSet<Capabilities> capabilities, @NotNull ClientQueryDelegate<T, U> delegate
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
                .whenComplete((entry, exc) -> {
                    if(entry != null) {
                        try {
                            entry.getClient().close();
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }
                })
                .thenApply(ExecutePair::getResult);
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

    @Override
    public void close() throws Exception {
        int count = clientCount.get();
        while(!clients.isEmpty() && count > 0) {
            clients.poll().client.disconnect().toCompletableFuture().get();
            count = clientCount.decrementAndGet();
        }
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

    private synchronized @NotNull CompletionStage<Void> onClientReady(@NotNull BaseEdgeDBClient client) {
        var suggestedConcurrency = client.getSuggestedPoolConcurrency();

        suggestedConcurrency.ifPresent(this.poolHolder::resize);

        return CompletableFuture.completedFuture(null);
    }

    private CompletionStage<BaseEdgeDBClient> createClient() {
        return this.poolHolder.acquireContract()
                .thenApply(contract -> {
                    logger.trace("Contract acquired, remaining handles: {}", this.poolHolder.remaining());
                    BaseEdgeDBClient client;
                    try {
                        client = clientFactory.create(this.connection, this.config, contract);
                    } catch (EdgeDBException e) {
                        throw new CompletionException(e);
                    }
                    contract.register(client, this::acceptClient);
                    client.onReady(this::onClientReady);
                    logger.debug("client instance created: {}", client);
                    return client;
                })
                .thenApply(client -> client.withSession(this.session));
    }

    @FunctionalInterface
    private interface ClientFactory {
        BaseEdgeDBClient create(EdgeDBConnection connection, EdgeDBClientConfig config, AutoCloseable poolHandle)
                throws EdgeDBException;
    }
}
