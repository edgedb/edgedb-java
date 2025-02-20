package com.gel.driver;

import com.gel.driver.abstractions.ClientQueryDelegate;
import com.gel.driver.clients.*;
import com.gel.driver.datatypes.Json;
import com.gel.driver.exceptions.ConfigurationException;
import com.gel.driver.exceptions.GelException;
import com.gel.driver.state.Config;
import com.gel.driver.state.Session;
import com.gel.driver.util.ClientPoolHolder;
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

import static com.gel.driver.util.ComposableUtil.composeWith;

/**
 * Represents a client pool used to interact with Gel.
 */
public final class GelClientPool implements StatefulClient, GelQueryable, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(GelClientPool.class);

    private static final class PooledClient {
        public final BaseGelClient client;
        public Instant lastUsed;

        public PooledClient(BaseGelClient client) {
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
    private final GelConnection connection;
    private final GelClientConfig config;
    private final ClientPoolHolder poolHolder;
    private final ClientFactory clientFactory;
    private final Session session;
    private final int clientAvailability;

    /**
     * Constructs a new {@linkplain GelClientPool}.
     * @param connection The connection parameters used to connect this client to Gel.
     * @param config The configuration for this client.
     * @throws ConfigurationException A configuration parameter is invalid.
     */
    public GelClientPool(
        @NotNull GelConnection connection,
        @NotNull GelClientConfig config
    ) throws ConfigurationException {
        this.clients = new ConcurrentLinkedQueue<>();
        this.config = config;
        this.connection = connection;
        this.poolHolder = new ClientPoolHolder(config.getPoolSize());
        this.clientFactory = createClientFactory();
        this.session = Session.DEFAULT;
        this.clientAvailability = config.getClientAvailability();
    }

    /**
     * Constructs a new {@linkplain GelClientPool}.
     * @param connection The connection parameters used to connect this client to Gel.
     * @throws ConfigurationException A configuration parameter is invalid.
     */
    public GelClientPool(@NotNull GelConnection connection) throws GelException {
        this(connection, GelClientConfig.DEFAULT);
    }

    /**
     * Constructs a new {@linkplain GelClientPool}.
     * @param config The configuration for this client.
     * @throws IOException The connection arguments couldn't be automatically resolved.
     * @throws ConfigurationException A configuration parameter is invalid.
     */
    public GelClientPool(@NotNull GelClientConfig config) throws IOException, ConfigurationException {
        this(GelConnection.builder().build(), config);
    }

    /**
     * Constructs a new {@linkplain GelClientPool}.
     * @throws IOException The connection arguments couldn't be automatically resolved.
     * @throws ConfigurationException A configuration parameter is invalid.
     */
    public GelClientPool() throws IOException, GelException {
        this(GelConnection.builder().build(), GelClientConfig.DEFAULT);
    }

    private GelClientPool(@NotNull GelClientPool other, Session session) {
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
            return GelTcpClient::new;
        } else if (config.getClientType() == ClientType.HTTP) {
            return GelHttpClient::new;
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
        return composeWith(getTransactableClient(), client -> client.transaction(settings, func));
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
    public @NotNull GelClientPool withSession(@NotNull Session session) {
        return new GelClientPool(this, session);
    }

    /**
     * Creates a new client instance with the specified module aliases.
     * <br/><br/>
     * The returned client shares the same underlying client pool as this client.
     * @param aliases The module aliases for the new client.
     * @return A new client instance with the applied module aliases, sharing the same underlying client pool.
     */
    @Override
    public @NotNull GelClientPool withModuleAliases(@NotNull Map<String, String> aliases) {
        return new GelClientPool(this, this.session.withModuleAliases(aliases));
    }

    /**
     * Creates a new client instance with the specified config.
     * <br/><br/>
     * The returned client shares the same underlying client pool as this client.
     * @param config The config for the new client.
     * @return A new client instance with the applied module aliases, sharing the same underlying client pool.
     */
    @Override
    public @NotNull GelClientPool withConfig(@NotNull Config config) {
        return new GelClientPool(this, this.session.withConfig(config));
    }

    /**
     * Creates a new client instance with the specified config builder.
     * <br/><br/>
     * The returned client shares the same underlying client pool as this client.
     * @param func A delegate that populates the config builder.
     * @return A new client instance with the applied module aliases, sharing the same underlying client pool.
     */
    @Override
    public @NotNull GelClientPool withConfig(@NotNull Consumer<Config.Builder> func) {
        return new GelClientPool(this, this.session.withConfig(func));
    }

    /**
     * Creates a new client instance with the specified globals.
     * <br/><br/>
     * The returned client shares the same underlying client pool as this client.
     * @param globals The globals for the new client.
     * @return A new client instance with the applied module aliases, sharing the same underlying client pool.
     */
    @Override
    public @NotNull GelClientPool withGlobals(@NotNull Map<String, Object> globals) {
        return new GelClientPool(this, this.session.withGlobals(globals));
    }

    /**
     * Creates a new client instance with the specified module
     * <br/><br/>
     * The returned client shares the same underlying client pool as this client.
     * @param module The module for the new client.
     * @return A new client instance with the applied module aliases, sharing the same underlying client pool.
     */
    @Override
    public @NotNull GelClientPool withModule(@NotNull String module) {
        return new GelClientPool(this, this.session.withModule(module));
    }

    // added because Map.entry cannot contain nulls
    private static final class ExecutePair<U> {
        private final BaseGelClient client;
        private final @Nullable U result;

        private ExecutePair(BaseGelClient client, @Nullable U result) {
            this.client = client;
            this.result = result;
        }

        public @Nullable U getResult() {
            return result;
        }

        public BaseGelClient getClient() {
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
        return executePooledQuery(cls, query, args, capabilities, GelQueryable::query);
    }

    @Override
    public <T> CompletionStage<T> querySingle(@NotNull Class<T> cls, @NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        return executePooledQuery(cls, query, args, capabilities, GelQueryable::querySingle);
    }

    @Override
    public <T> CompletionStage<T> queryRequiredSingle(@NotNull Class<T> cls, @NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        return executePooledQuery(cls, query, args, capabilities, GelQueryable::queryRequiredSingle);
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

    private synchronized CompletionStage<BaseGelClient> getClient() {
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
                                new GelException("Cannot use transactions with " + client + " type")
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

    private synchronized void acceptClient(BaseGelClient client) {
        this.clients.add(new PooledClient(client));
        var count = this.clientCount.incrementAndGet();

        logger.debug("client {} returned to pool, client count: {}", client, count);

        if(count > this.clientAvailability) {
            logger.debug("Cleaning up pool... {}/{} availability reached", count, this.clientAvailability);
            cleanupPool();
        }
    }

    private synchronized @NotNull CompletionStage<Void> onClientReady(@NotNull BaseGelClient client) {
        var suggestedConcurrency = client.getSuggestedPoolConcurrency();

        suggestedConcurrency.ifPresent(this.poolHolder::resize);

        return CompletableFuture.completedFuture(null);
    }

    private CompletionStage<BaseGelClient> createClient() {
        return this.poolHolder.acquireContract()
                .thenApply(contract -> {
                    logger.trace("Contract acquired, remaining handles: {}", this.poolHolder.remaining());
                    BaseGelClient client;
                    try {
                        client = clientFactory.create(this.connection, this.config, contract);
                    } catch (GelException e) {
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
        BaseGelClient create(GelConnection connection, GelClientConfig config, AutoCloseable poolHandle)
                throws GelException;
    }
}
