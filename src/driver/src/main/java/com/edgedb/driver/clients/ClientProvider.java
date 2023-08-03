package com.edgedb.driver.clients;

import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a generic edgedb client provider.
 */
public abstract class ClientProvider {
    private static final Logger logger = LoggerFactory.getLogger(ClientProvider.class);
    protected static final class PooledClient {
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


    protected final AtomicInteger clientCount = new AtomicInteger();
    protected final @NotNull ConcurrentLinkedQueue<PooledClient> clients;

    private final int clientAvailability;
    private final Duration clientMaxAge;

    /**
     * Constructs this client provider.
     * @param clientMaxAge The max age of clients within this providers cache.
     * @param availability The number of clients that should be available at any given time.
     */
    public ClientProvider(Duration clientMaxAge, int availability) {
        this.clients = new ConcurrentLinkedQueue<>();
        this.clientAvailability = availability;
        this.clientMaxAge = clientMaxAge;
    }

    protected abstract CompletionStage<BaseEdgeDBClient> getClient();

    /**
     * Gets a client from this provider. This method is not meant for public use.
     * @param cls The type of client to get.
     * @return A client, as the specified type.
     * @param <T> The client type to get.
     */
    public <T extends BaseEdgeDBClient> CompletionStage<T> getClient(Class<T> cls) {
        return getClient()
                .thenCompose(client -> {
                    if(!client.getClass().equals(cls)) {
                        return CompletableFuture.failedFuture(
                                new EdgeDBException("Client type mismatch: expected " + cls.getName() + ", but got " + client.getClass().getName())
                        );
                    }

                    //noinspection unchecked
                    return CompletableFuture.completedFuture((T)client);
                });
    }


    protected synchronized CompletionStage<TransactableClient> getTransactableClient() {
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

    protected void cleanupPool() {
        clients.removeIf(c ->
                c.age().compareTo(clientMaxAge) > 0
                        || (!c.client.isConnected() && this.clientCount.decrementAndGet() >= this.clientAvailability)
        );
    }

    protected synchronized void acceptClient(BaseEdgeDBClient client) {
        this.clients.add(new PooledClient(client));
        var count = this.clientCount.incrementAndGet();

        logger.debug("client {} returned to pool, client count: {}", client, count);

        if(count > this.clientAvailability) {
            logger.debug("Cleaning up pool... {}/{} availability reached", count, this.clientAvailability);
            cleanupPool();
        }
    }
}
