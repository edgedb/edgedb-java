package com.edgedb.driver.clients;

import com.edgedb.driver.*;
import com.edgedb.driver.binary.builders.CodecBuilder;
import com.edgedb.driver.binary.builders.ObjectBuilder;
import com.edgedb.driver.binary.builders.types.TypeBuilder;
import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.duplexers.Duplexer;
import com.edgedb.driver.binary.protocol.*;
import com.edgedb.driver.binary.protocol.common.Cardinality;
import com.edgedb.driver.binary.protocol.common.IOFormat;
import com.edgedb.driver.datatypes.Json;
import com.edgedb.driver.exceptions.ConnectionFailedException;
import com.edgedb.driver.exceptions.EdgeDBErrorException;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.exceptions.ResultCardinalityMismatchException;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joou.UShort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.edgedb.driver.util.ComposableUtil.exceptionallyCompose;

public abstract class EdgeDBBinaryClient extends BaseEdgeDBClient {
    private static final Logger logger = LoggerFactory.getLogger(EdgeDBBinaryClient.class);
    @SuppressWarnings("rawtypes")
    private @Nullable Codec<Map> stateCodec;
    private UUID stateDescriptorId;
    private @Nullable Long suggestedPoolConcurrency;

    private @NotNull ProtocolProvider protocolProvider;
    private short connectionAttempts;
    private final @NotNull Semaphore connectionSemaphore;
    private final @NotNull Semaphore querySemaphore;
    private @NotNull CompletableFuture<Void> readyPromise;
    private final CodecContext codecContext = new CodecContext(this);

    public EdgeDBBinaryClient(GelConnection connection, GelClientConfig config, AutoCloseable poolHandle) {
        super(connection, config, poolHandle);
        this.connectionSemaphore = new Semaphore(1);
        this.querySemaphore = new Semaphore(1);
        this.readyPromise = new CompletableFuture<>();
        this.stateDescriptorId = CodecBuilder.INVALID_CODEC_ID;
        this.protocolProvider = ProtocolProvider.getProvider(this);
    }

    public abstract Duplexer getDuplexer();

    public @NotNull ProtocolProvider getProtocolProvider() {
        return this.protocolProvider;
    }

    @Override
    public @NotNull Optional<Long> getSuggestedPoolConcurrency() {
        return Optional.ofNullable(this.suggestedPoolConcurrency);
    }

    public void setSuggestedPoolConcurrency(long value) {
        this.suggestedPoolConcurrency = value;
    }

    public @NotNull CodecContext getCodecContext() {
        return this.codecContext;
    }

    public UUID getStateDescriptorId() {
        return this.stateDescriptorId;
    }
    public void setStateDescriptorId(UUID id) {
        this.stateDescriptorId = id;
    }

    @SuppressWarnings("rawtypes")
    public void setStateCodec(@Nullable Codec<Map> codec) {
        this.stateCodec = codec;
    }

    private static class ExecutionState {
        public int attempts;
    }

    public final CompletionStage<ExecuteResult> executeQuery(
            @NotNull QueryParameters args
    ) {
        logger.debug("Execute request: is connected? {}", getDuplexer().isConnected());

        if(!getDuplexer().isConnected()) {
            // TODO: check for recursion
            return connect()
                    .thenCompose(v -> executeQuery(args));
        }

        final var hasReleased = new AtomicBoolean();
        final var executionState = new ExecutionState();

        return CompletableFuture.runAsync(() -> {
                    try {
                        logger.debug("acquiring query semaphore...");
                        this.querySemaphore.acquire();
                        logger.debug("query semaphore acquired");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenCompose((v) -> executeQuery0(args,executionState))
                .whenComplete((v,e) -> {
                    if(!hasReleased.get()) {
                        this.querySemaphore.release();
                    }
                });
    }

    private CompletionStage<ExecuteResult> executeQuery0(@NotNull QueryParameters args, ExecutionState state) {
        return exceptionallyCompose(
                protocolProvider
                        .parseQuery(args)
                        .thenCompose(parseResult -> protocolProvider.executeQuery(args, parseResult)),
                e -> {
                    logger.debug("got exception in execute step", e);

                    if(e instanceof EdgeDBErrorException && !((EdgeDBException)e).shouldReconnect && !((EdgeDBException)e).shouldRetry) {
                        return CompletableFuture.failedFuture(e);
                    }

                    if(e instanceof EdgeDBException) {
                        var edbException = (EdgeDBException) e;
                        if(state.attempts > getConfig().getMaxConnectionRetries()) {
                            return CompletableFuture.failedFuture(
                                    new EdgeDBException(
                                            String.format(
                                                    "Failed to execute query after %d attempts",
                                                    state.attempts
                                            ),
                                            e
                                    )
                            );
                        }

                        if(edbException.shouldRetry) {
                            state.attempts++;
                            logger.debug("Retrying with attempts now at {}", state.attempts);

                            return executeQuery0(args, state);
                        }

                        if(edbException.shouldReconnect) {
                            state.attempts++;
                            logger.debug("Reconnecting and retrying with attempts now at {}", state.attempts);

                            return this.reconnect()
                                    .thenCompose(y -> executeQuery0(args, state));
                        }
                    }

                    return CompletableFuture.failedFuture(new EdgeDBException("Failed to execute query", e));
                }
        );
    }

    @Override
    public CompletionStage<Void> execute(
            @NotNull String query,
            @Nullable Map<String, @Nullable Object> args,
            EnumSet<Capabilities> capabilities
    ) {
        return executeQuery(new QueryParameters(
                query,
                args,
                capabilities,
                Cardinality.MANY,
                IOFormat.NONE,
                false
        )).thenApply(r -> null);
    }

    @Override
    public <T> CompletionStage<List<T>> query(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        return executeQuery(new QueryParameters(
                query,
                args,
                capabilities,
                Cardinality.MANY,
                IOFormat.BINARY,
                TypeBuilder.requiredImplicitTypeNames(cls)
        )).thenCompose(result -> {
            var arr = new ArrayList<T>(result.data.size());

            for(int i = 0; i != result.data.size(); i++) {
                try {
                    arr.add(
                            i,
                            ObjectBuilder.buildResult(
                                    this,
                                    result.codec,
                                    result.data.get(i),
                                    cls
                            )
                    );
                } catch (EdgeDBException | OperationNotSupportedException e) {
                    return CompletableFuture.failedFuture(e);
                } finally {
                    // free the buffer
                    result.data.get(i).release();
                }
            }

            return CompletableFuture.completedFuture(Collections.unmodifiableList(arr));
        });
    }

    @Override
    public <T> CompletionStage<T> querySingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        return executeQuery(new QueryParameters(
                query,
                args,
                capabilities,
                Cardinality.MANY,
                IOFormat.BINARY,
                TypeBuilder.requiredImplicitTypeNames(cls)
        )).thenApply(result -> {
            if(result.data.size() == 0) {
                return null;
            }

            if(result.data.size() > 1) {
                for (var buffer : result.data) {
                    buffer.release();
                }

                throw new CompletionException(
                        new ResultCardinalityMismatchException(Cardinality.AT_MOST_ONE, Cardinality.MANY)
                );
            }

            try {
                return ObjectBuilder.buildResult(
                        this,
                        result.codec,
                        result.data.get(0),
                        cls
                );
            } catch (EdgeDBException | OperationNotSupportedException e) {
                throw new CompletionException(e);
            }
            finally {
                result.data.get(0).release();
            }
        });

    }

    @Override
    public <T> CompletionStage<T> queryRequiredSingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    ) {

        return executeQuery(new QueryParameters(
                query,
                args,
                capabilities,
                Cardinality.MANY,
                IOFormat.BINARY,
                TypeBuilder.requiredImplicitTypeNames(cls)
        )).thenApply(result -> {
            if(result.data.size() != 1) {
                for (var buffer : result.data) {
                    buffer.release();
                }

                throw new CompletionException(
                        new ResultCardinalityMismatchException(Cardinality.ONE, Cardinality.MANY)
                );
            }

            try {
                return ObjectBuilder.buildResult(
                        this,
                        result.codec,
                        result.data.get(0),
                        cls
                );
            } catch (EdgeDBException | OperationNotSupportedException e) {
                throw new CompletionException(e);
            }
            finally {
                result.data.get(0).release();
            }
        });
    }

    @Override
    public CompletionStage<Json> queryJson(
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        return executeQuery(new QueryParameters(
                query,
                args,
                capabilities,
                Cardinality.MANY,
                IOFormat.JSON,
                false
        )).thenApply(result -> {
            if(result.data.size() > 1) {
                for (var buffer : result.data) {
                    buffer.release();
                }

                throw new CompletionException(
                        new ResultCardinalityMismatchException(Cardinality.AT_MOST_ONE, Cardinality.MANY)
                );
            }

            if(result.data.size() == 0) {
                return new Json("[]");
            }

            try {
                return new Json(
                        (String)Objects.requireNonNull(Codec.deserializeFromBuffer(
                                result.codec,
                                Objects.requireNonNull(result.data.get(0)),
                                this.codecContext
                        ))
                );
            } catch (EdgeDBException | OperationNotSupportedException e) {
                throw new CompletionException(e);
            }
            finally {
                result.data.get(0).release();
            }
        });
    }

    @Override
    public CompletionStage<List<Json>> queryJsonElements(@NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        return executeQuery(new QueryParameters(
                query,
                args,
                capabilities,
                Cardinality.MANY,
                IOFormat.JSON_ELEMENTS,
                false
        )).thenApply(result -> {
            try {
                var data = new Json[result.data.size()];

                for(int i = 0; i != result.data.size(); i++) {
                    try {
                        data[i] = new Json(
                                (String) Objects.requireNonNull(Codec.deserializeFromBuffer(
                                        result.codec,
                                        Objects.requireNonNull(result.data.get(i)),
                                        this.codecContext
                                ))
                        );
                    } catch (EdgeDBException | OperationNotSupportedException e) {
                        throw new CompletionException(e);
                    }
                }

                return List.of(data);
            }
            finally {
                for (var buffer : result.data) {
                    buffer.release();
                }
            }
        });
    }

    @Nullable
    public ByteBuf serializeState() throws OperationNotSupportedException, EdgeDBException {
        if(this.stateCodec == null) {
            return null;
        }

        var data = session.serialize();
        return Codec.serializeToBuffer(this.stateCodec, data, this.codecContext);
    }

    public boolean tryNegotiateProtocol(UShort major, UShort minor) {
        logger.info("Server requested protocol {}.{}, current: {}", major, minor, protocolProvider.getVersion());

        var newProvider = ProtocolProvider.PROVIDERS.get(ProtocolVersion.of(major, minor));
        if(newProvider != null) {
            this.protocolProvider = newProvider.apply(this);
            logger.debug("Protocol provider found, using {}", this.protocolProvider);
            ProtocolProvider.updateProviderFor(this, this.protocolProvider);
            return true;
        }
        logger.debug("No provider found for {}.{}", major, minor);
        return false;
    }

    public void cancelReadyState(@Nullable Exception error) {
        if(error == null) {
            readyPromise.cancel(true);
        } else {
            readyPromise.completeExceptionally(error);
        }
    }

    @Override
    public CompletionStage<Void> connect() {
        return CompletableFuture
                .runAsync(() -> {
                    try {
                        logger.debug("Acquiring connection lock...");
                        if(!this.connectionSemaphore.tryAcquire(
                            getConnectionArguments().getWaitUntilAvailable().value,
                            getConnectionArguments().getWaitUntilAvailable().unit
                        )) {
                            logger.debug("Failed to acquire connection lock after timeout");
                            throw new CompletionException(new ConnectionFailedException("Connection failed to be established because of a already existing attempt"));
                        }
                    } catch (InterruptedException e) {
                        throw new CompletionException(e);
                    }
                })
                .thenCompose((v) -> this.connectInternal())
                .thenRunAsync(this::doClientHandshake)
                .thenCompose((v) -> this.readyPromise)
                .thenAccept(v -> this.connectionAttempts = 0)
                .whenComplete((v,e) -> this.connectionSemaphore.release());
    }

    private CompletionStage<Void> doClientHandshake() {
        return exceptionallyCompose(
                getDuplexer()
                        .readNext()
                        .thenCompose(protocolProvider::processMessage)
                        .thenCompose(v -> {
                            logger.debug("Protocol phase: {}", protocolProvider.getPhase());

                            if(protocolProvider.getPhase() != ProtocolPhase.COMMAND) {
                                logger.debug("Rerunning handshake, phase: {}", protocolProvider.getPhase());
                                return doClientHandshake();
                            }

                            this.readyPromise.complete(null);
                            return dispatchReady();
                        }),
                error -> {
                    if(error instanceof EdgeDBErrorException && ((EdgeDBException)error).shouldReconnect) {
                        if(getConfig().getConnectionRetryMode() == ConnectionRetryMode.NEVER_RETRY) {
                            return CompletableFuture.failedFuture(new ConnectionFailedException(error));
                        }

                        if(this.connectionAttempts < getConfig().getMaxConnectionRetries()) {
                            this.connectionAttempts++;

                            logger.warn("Attempting to reconnect... {}/{}", this.connectionAttempts, getConfig().getMaxConnectionRetries(), error);

                            return disconnect()
                                    .thenCompose(v -> {
                                        this.connectionSemaphore.release();
                                        return connect();
                                    });
                        } else {
                            logger.error("Failed to establish a connection after {} attempts", this.connectionAttempts, error);
                            this.connectionAttempts = 0;
                            return CompletableFuture.failedFuture(new ConnectionFailedException(this.connectionAttempts, error));
                        }
                    }

                    return CompletableFuture.failedFuture(error);
                }
        );
    }

    @Override
    public CompletionStage<Void> disconnect() {
        return getDuplexer().disconnect();
    }

    private CompletionStage<Void> connectInternal() {
        logger.debug("Beginning to run connection logic");
        if(getDuplexer().isConnected()) {
            logger.debug("Already connected, ignoring connection attempt");
            return CompletableFuture.completedFuture(null);
        }

        logger.debug("Resetting ready state");
        getDuplexer().reset();
        this.readyPromise = new CompletableFuture<>();

        return retryableConnect()
                .thenApply(v -> protocolProvider.handshake())
                .thenCompose(getDuplexer()::send);
    }

    private CompletionStage<Void> retryableConnect() {
        try {
            return exceptionallyCompose(this.openConnection(), err -> {
                logger.debug("Connection attempt failed", err);
                if(err instanceof EdgeDBException && ((EdgeDBException)err).shouldReconnect) {
                    if(getConfig().getConnectionRetryMode() == ConnectionRetryMode.NEVER_RETRY) {
                        return CompletableFuture.failedFuture(new ConnectionFailedException(err));
                    }

                    if(this.connectionAttempts < getConfig().getMaxConnectionRetries()) {
                        this.connectionAttempts++;

                        logger.warn("Attempting to reconnect... {}/{}", this.connectionAttempts, getConfig().getMaxConnectionRetries(), err);

                        return disconnect()
                                .thenCompose(v -> retryableConnect());
                    } else {
                        logger.error("Failed to establish a connection after {} attempts", this.connectionAttempts, err);
                        this.connectionAttempts = 0;
                        return CompletableFuture.failedFuture(new ConnectionFailedException(this.connectionAttempts, err));
                    }
                }

                return CompletableFuture.failedFuture(err);
            });
        }
        catch (Exception x) {
            logger.debug("Connection failed", x);
            return CompletableFuture.failedFuture(x);
        }
    }

    public abstract void setTransactionState(TransactionState state);

    protected abstract CompletionStage<Void> openConnection();
    protected abstract CompletionStage<Void> closeConnection();

    @Override
    public boolean isConnected() {
        return getDuplexer().isConnected();
    }
}
