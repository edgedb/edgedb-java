package com.edgedb.driver.clients;

import com.edgedb.driver.*;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.builders.CodecBuilder;
import com.edgedb.driver.binary.builders.ObjectBuilder;
import com.edgedb.driver.binary.builders.types.TypeBuilder;
import com.edgedb.driver.binary.codecs.ArgumentCodec;
import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.duplexers.Duplexer;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.receivable.*;
import com.edgedb.driver.binary.packets.sendables.ClientHandshake;
import com.edgedb.driver.binary.packets.sendables.Execute;
import com.edgedb.driver.binary.packets.sendables.Parse;
import com.edgedb.driver.binary.packets.shared.*;
import com.edgedb.driver.datatypes.Json;
import com.edgedb.driver.exceptions.*;
import com.edgedb.driver.util.BinaryProtocolUtils;
import com.edgedb.driver.util.Scram;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joou.UShort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.edgedb.driver.async.ExceptionallyCompose.exceptionallyCompose;
import static org.joou.Unsigned.ushort;

@SuppressWarnings("ClassEscapesDefinedScope")
public abstract class EdgeDBBinaryClient extends BaseEdgeDBClient {
    private static final Logger logger = LoggerFactory.getLogger(EdgeDBBinaryClient.class);

    private static final UShort PROTOCOL_MAJOR_VERSION = ushort(1);
    private static final UShort PROTOCOL_MINOR_VERSION = ushort(0);

    private static final int MAX_PARSE_ATTEMPTS = 2;

    public @Nullable Long suggestedPoolConcurrency;
    public Map<String, Object> rawServerConfig;

    @SuppressWarnings("rawtypes")
    private Codec<Map> stateCodec;
    private UUID stateDescriptorId;

    private byte[] serverKey;

    private short connectionAttempts;

    protected Duplexer duplexer;
    private boolean isIdle;
    private final Semaphore connectionSemaphore;
    private final Semaphore querySemaphore;
    private final CompletableFuture<Void> readyPromise;
    private final CodecContext codecContext = new CodecContext(this);

    public EdgeDBBinaryClient(EdgeDBConnection connection, EdgeDBClientConfig config, AutoCloseable poolHandle) {
        super(connection, config, poolHandle);
        this.connectionSemaphore = new Semaphore(1);
        this.querySemaphore = new Semaphore(1);
        this.readyPromise = new CompletableFuture<>();
        this.stateDescriptorId = CodecBuilder.INVALID_CODEC_ID;
    }

    @Override
    public Optional<Long> getSuggestedPoolConcurrency() {
        return Optional.ofNullable(this.suggestedPoolConcurrency);
    }

    @Override
    public Map<String, Object> getServerConfig() {
        return rawServerConfig;
    }

    public CodecContext getCodecContext() {
        return this.codecContext;
    }

    public boolean getIsIdle() {
        return isIdle;
    }

    protected void setIsIdle(boolean value) {
        isIdle = value;
    }

    protected void setDuplexer(Duplexer duplexer) {
        this.duplexer = duplexer;
    }

    private CompletionStage<Void> parse(ExecutionArguments args) {
        return runWithAttempts(
                args,
                this::parse0,
                ExecutionArguments::isParseComplete,
                () -> args.parseAttempts++
        );
    }
    private CompletionStage<Void> parse0(ExecutionArguments args) {
        if(args.parseAttempts > MAX_PARSE_ATTEMPTS) {
            logger.debug("Parse attempts exceeded {}", MAX_PARSE_ATTEMPTS);
            return CompletableFuture.failedFuture(
                    new EdgeDBException("Failed to parse query after " + args.parseAttempts + " attempts")
            );
        }

        logger.debug("Starting to parse... attempt {}/{}", args.parseAttempts + 1, MAX_PARSE_ATTEMPTS);

        try {
            return duplexer.duplexAndSync(args.toParsePacket(), (result) -> {
                switch (result.packet.getMessageType()) {
                    case ERROR_RESPONSE:
                        var err = result.packet.as(ErrorResponse.class);
                        logger.debug("handling error: {}", err.errorCode);
                        handleCommandError(args, result, err);
                        return CompletableFuture.completedFuture(null);
                    case COMMAND_DATA_DESCRIPTION:
                        var commandDescriptor = result.packet.as(CommandDataDescription.class);

                        logger.debug("parsing command data description");

                        if(!args.capabilities.equals(commandDescriptor.capabilities)) {
                            logger.debug(
                                    "actual capabilities differ from the provided ones: provided: {}. actual: {}",
                                    args.capabilities,
                                    commandDescriptor.capabilities
                            );
                            args.actualCapabilities = commandDescriptor.capabilities;
                        }

                        if(args.cardinality != commandDescriptor.cardinality) {
                            logger.debug(
                                    "actual cardinality differs from the provided one: provided: {}. actual: {}",
                                    args.cardinality,
                                    commandDescriptor.cardinality
                            );
                            args.actualCardinality = commandDescriptor.cardinality;
                        }

                        try {
                            args.codecs = new CodecBuilder.QueryCodecs(
                                    commandDescriptor.inputTypeDescriptorId,
                                    CodecBuilder.buildCodec(
                                            this,
                                            commandDescriptor.inputTypeDescriptorId,
                                            commandDescriptor.inputTypeDescriptorBuffer
                                    ),
                                    commandDescriptor.outputTypeDescriptorId,
                                    CodecBuilder.buildCodec(
                                            this,
                                            commandDescriptor.outputTypeDescriptorId,
                                            commandDescriptor.outputTypeDescriptorBuffer
                                    )
                            );

                            logger.debug(
                                    "updating codec query cache key {} with I:{} O:{}",
                                    args.getCacheKey(),
                                    commandDescriptor.inputTypeDescriptorId,
                                    commandDescriptor.outputTypeDescriptorId
                            );

                            CodecBuilder.updateCachedCodecs(
                                    args.getCacheKey(),
                                    commandDescriptor.inputTypeDescriptorId,
                                    commandDescriptor.outputTypeDescriptorId
                            );
                        } catch (EdgeDBException | OperationNotSupportedException e) {
                            logger.debug("codec building failed", e);
                            result.finishExceptionally("Failed to parse in/out codecs", e, EdgeDBException::new);
                        }
                        break;
                    case STATE_DATA_DESCRIPTION:
                        updateStateCodec(result, args);
                        break;
                    case READY_FOR_COMMAND:
                        var ready = result.packet.as(ReadyForCommand.class);
                        setTransactionState(ready.transactionState);
                        args.completedParse = true;
                        result.finishDuplexing();
                }

                return CompletableFuture.completedFuture(null);
            });
        } catch (SSLException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletionStage<Void> execute(ExecutionArguments args) {
        return runWithAttempts(
                args,
                this::execute0,
                ExecutionArguments::isExecuteComplete,
                () -> args.parseAttempts++
        );
    }
    private CompletionStage<Void> execute0(ExecutionArguments args) {
        if(args.parseAttempts > 2) {
            return CompletableFuture.failedFuture(
                    new EdgeDBException("Failed to parse query after " + args.parseAttempts + " attempts")
            );
        }

        if(!(args.codecs.inputCodec instanceof ArgumentCodec)) {
            return CompletableFuture.failedFuture(
                    new MissingCodecException(String.format(
                            "Cannot encode arguments, %s is not a valid argument codec",
                            args.codecs.inputCodec)
                    )
            );
        }

        try {
            return duplexer.duplexAndSync(args.toExecutePacket(), (result) -> {
                switch (result.packet.getMessageType()) {
                    case DATA:
                        args.data.add(result.packet.as(Data.class));
                        break;
                    case STATE_DATA_DESCRIPTION:
                        updateStateCodec(result, args);
                        break;
                    case ERROR_RESPONSE:
                        var err = result.packet.as(ErrorResponse.class);
                        handleCommandError(args, result, err);
                        break;
                    case READY_FOR_COMMAND:
                        var ready = result.packet.as(ReadyForCommand.class);
                        setTransactionState(ready.transactionState);
                        args.completedExecute = true;
                        result.finishDuplexing();
                        break;
                }

                return CompletableFuture.completedFuture(null);
            });
        } catch (SSLException | OperationNotSupportedException | EdgeDBException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletionStage<Void> runWithAttempts(
            ExecutionArguments args,
            Function<ExecutionArguments, CompletionStage<Void>> delegate,
            Predicate<ExecutionArguments> completedPredicate,
            Runnable incrementer
    ) {
        return delegate.apply(args)
                .thenCompose((v) -> {
                    if(!completedPredicate.test(args)) {
                        incrementer.run();
                        return runWithAttempts(args, delegate, completedPredicate, incrementer);
                    }

                    return CompletableFuture.completedFuture(null);
                });
    }

    private void handleCommandError(ExecutionArguments args, Duplexer.DuplexResult result, ErrorResponse err) {
        if(err.errorCode == ErrorCode.STATE_MISMATCH_ERROR) {
            // should have new state
            if(!args.stateUpdated) {
                result.finishExceptionally(
                        "Failed to properly encode state data, this is a bug",
                        EdgeDBException::new
                );
                return;
            }

            result.finishDuplexing();
        }
        else {
            result.finishExceptionally(err, args.query, EdgeDBErrorException::fromError);
        }
    }

    private void updateStateCodec(Duplexer.DuplexResult result, ExecutionArguments args) {
        var stateDescriptor = result.packet.as(StateDataDescription.class);
        var codec = CodecBuilder.getCodec(stateDescriptor.typeDescriptorId, Map.class);

        if(codec == null) {
            try {
                codec = CodecBuilder.buildCodec(
                        this,
                        stateDescriptor.typeDescriptorId,
                        stateDescriptor.typeDescriptorBuffer,
                        Map.class
                );

                if(codec == null) {
                    throw new MissingCodecException("Failed to build state codec");
                }
            } catch (EdgeDBException | OperationNotSupportedException e) {
                result.finishExceptionally("Failed to parse state codec", e, EdgeDBException::new);
            }
        }

        stateCodec = codec;
        stateDescriptorId = stateDescriptor.typeDescriptorId;
        args.stateUpdated = true;

        try {
            args.setStateData(serializeState());
        } catch (OperationNotSupportedException | EdgeDBException e) {
            result.finishExceptionally("Failed to serialize state", e, EdgeDBException::new);
        }
    }

    public final CompletionStage<Void> executeQuery(
            ExecutionArguments args
    ) {
        logger.debug("Execute request: is connected? {}", duplexer.isConnected());
        if(!duplexer.isConnected()) {
            // TODO: check for recursion
            return reconnect()
                    .thenCompose(v -> executeQuery(args));
        }

        final var hasReleased = new AtomicBoolean();

        return CompletableFuture.runAsync(() -> {
                    try {
                        this.querySemaphore.acquire();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenCompose((v) -> {
                    logger.debug("Entering execute step");

                    CompletionStage<Void> task;

                    try {
                        var cacheKey = args.getCacheKey();

                        args.setStateData(serializeState());

                        var codecInfo = CodecBuilder.getCachedCodecs(cacheKey);

                        if(codecInfo == null) {
                            task = parse(args).thenCompose((u) -> execute(args));
                        } else {
                            args.codecs = codecInfo;

                            logger.debug("Codecs found for query: {} {}", codecInfo.inputCodec, codecInfo.outputCodec);

                            task = execute(args);
                        }
                    } catch (OperationNotSupportedException | EdgeDBException e) {
                        throw new RuntimeException(e);
                    }

                    return exceptionallyCompose(task, e -> {
                        if(e instanceof EdgeDBErrorException && !((EdgeDBException)e).shouldReconnect && !((EdgeDBException)e).shouldRetry) {
                            return CompletableFuture.failedFuture(e);
                        }

                        if(e instanceof EdgeDBException) {
                            var edbException = (EdgeDBException) e;
                            if(edbException.shouldRetry && !args.isRetry) {
                                this.querySemaphore.release();
                                hasReleased.set(true);
                                var newArgs = args.deriveNew();
                                newArgs.isRetry = true;
                                return executeQuery(newArgs);
                            }

                            if(edbException.shouldReconnect && !args.isRetry) {
                                var newArgs = args.deriveNew();
                                newArgs.isRetry = true;

                                return this.reconnect()
                                        .thenAccept(x -> {
                                            this.querySemaphore.release();
                                            hasReleased.set(true);
                                        })
                                        .thenCompose(y -> executeQuery(newArgs));
                            }
                        }

                        var sb = new StringBuilder("Failed to execute query");

                        if(args.isRetry) {
                            sb.append(" after retrying once");
                        }

                        return CompletableFuture.failedFuture(new EdgeDBException(sb.toString(), e));
                    });

                })
                .whenComplete((v,e) -> {
                    if(!hasReleased.get()) {
                        this.querySemaphore.release();
                    }
                });
    }

    @Override
    public CompletionStage<Void> execute(
            @NotNull String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities
    ) {
        return executeQuery(new ExecutionArguments(
                query,
                args,
                Cardinality.MANY,
                capabilities,
                IOFormat.NONE,
                false,
                false
        ));
    }

    @Override
    public <T> CompletionStage<List<T>> query(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        // TODO: does this query result require implicit type names
        final var executeArgs = new ExecutionArguments(
                query,
                args,
                Cardinality.MANY,
                capabilities,
                IOFormat.BINARY,
                false,
                TypeBuilder.requiredImplicitTypeNames(cls)
        );

        return executeQuery(executeArgs)
                .thenCompose((v) -> {
                    var arr = new ArrayList<T>(executeArgs.data.size());

                    for(int i = 0; i != executeArgs.data.size(); i++) {
                        try {
                            arr.add(i, ObjectBuilder.buildResult(
                                    this,
                                    executeArgs.codecs.outputCodec,
                                    executeArgs.data.get(i),
                                    cls
                            ));
                        } catch (EdgeDBException | OperationNotSupportedException e) {
                            return CompletableFuture.failedFuture(e);
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
        // TODO: does this query result require implicit type names
        final var executeArgs = new ExecutionArguments(
                query,
                args,
                Cardinality.MANY,
                capabilities,
                IOFormat.BINARY,
                false,
                TypeBuilder.requiredImplicitTypeNames(cls)
        );

        return executeQuery(executeArgs)
                .thenCompose((v) -> deserializeSingleResult(cls, executeArgs, Cardinality.AT_MOST_ONE));
    }

    @Override
    public <T> CompletionStage<T> queryRequiredSingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        // TODO: does this query result require implicit type names
        final var executeArgs = new ExecutionArguments(
                query,
                args,
                Cardinality.MANY,
                capabilities,
                IOFormat.BINARY,
                false,
                TypeBuilder.requiredImplicitTypeNames(cls)
        );

        return executeQuery(executeArgs)
                .thenCompose((v) -> deserializeSingleResult(cls, executeArgs, Cardinality.ONE));
    }

    @Override
    public CompletionStage<Json> queryJson(
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        final var executeArgs = new ExecutionArguments(
                query,
                args,
                Cardinality.MANY,
                capabilities,
                IOFormat.JSON,
                false,
                false
        );

        return executeQuery(executeArgs)
                .thenCompose(v -> deserializeSingleResult(String.class, executeArgs, Cardinality.AT_MOST_ONE))
                .thenApply(Json::new);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletionStage<List<Json>> queryJsonElements(@NotNull String query, @Nullable Map<String, Object> args, @NotNull EnumSet<Capabilities> capabilities) {
        final var executeArgs = new ExecutionArguments(
                query,
                args,
                Cardinality.MANY,
                capabilities,
                IOFormat.JSON_ELEMENTS,
                false,
                false
        );

        return executeQuery(executeArgs)
                .thenApply(v -> {
                    var result = new Json[executeArgs.data.size()];

                    for(int i = 0; i != executeArgs.data.size(); i++) {
                        try {
                            result[i] = new Json(
                                    (String) Codec.deserializeFromBuffer(
                                            executeArgs.codecs.outputCodec,
                                            executeArgs.data.get(i).payloadBuffer,
                                            this.codecContext
                                    )
                            );
                        } catch (EdgeDBException | OperationNotSupportedException e) {
                            throw new CompletionException(e);
                        }
                    }

                    return List.of(result);
                });
    }

    @SuppressWarnings("unchecked")
    private <T> CompletionStage<T> deserializeSingleResult(Class<T> cls, ExecutionArguments args, Cardinality expected) {
        switch (args.format) {
            case JSON:
                if(args.data.size() == 1) {
                    try {
                        return CompletableFuture.completedFuture(
                                Codec.deserializeFromBuffer(
                                        (Codec<T>)args.codecs.outputCodec,
                                        args.data.get(0).payloadBuffer,
                                        this.codecContext
                                )
                        );
                    }
                    catch (Exception x) {
                        return CompletableFuture.failedFuture(x);
                    }
                }

                return CompletableFuture.completedFuture((T)"[]");
            case BINARY:
                switch (expected) {
                    case ONE:
                    case AT_MOST_ONE:
                        if(expected == Cardinality.ONE
                            ? args.data.size() != 1
                            : args.data.size() > 1
                        ) {
                            return CompletableFuture.failedFuture(
                                    new ResultCardinalityMismatchException(
                                            expected,
                                            args.data.size() > 1
                                                    ? Cardinality.MANY
                                                    : Cardinality.AT_MOST_ONE
                                    )
                            );
                        }

                        try {
                            return CompletableFuture.completedFuture(
                                    args.data.size() == 0
                                            ? null
                                            : ObjectBuilder.buildResult(
                                                this,
                                                args.codecs.outputCodec,
                                                args.data.get(0),
                                                cls
                                            )
                            );
                        } catch (EdgeDBException | OperationNotSupportedException e) {
                            return CompletableFuture.failedFuture(e);
                        }
                    default:
                        return CompletableFuture.failedFuture(
                                new EdgeDBException("Unsupported cardinality result " + expected)
                        );
                }
            default:
                return CompletableFuture.failedFuture(
                        new EdgeDBException("Unsupported IO format" + args.format)
                );
        }
    }

    private @Nullable ByteBuf serializeState() throws OperationNotSupportedException, EdgeDBException {
        if(this.stateCodec == null) {
            return null;
        }

        var data = session.serialize();
        return Codec.serializeToBuffer(this.stateCodec, data, this.codecContext);
    }

    private CompletionStage<Void> handlePacket(
            Receivable packet
    ) throws SSLException, EdgeDBException, OperationNotSupportedException {
        switch (packet.getMessageType()) {
            case SERVER_HANDSHAKE:
                var handshake = (ServerHandshake)packet;

                if(handshake.majorVersion.compareTo(PROTOCOL_MAJOR_VERSION) != 0 ||
                        handshake.minorVersion.compareTo(PROTOCOL_MINOR_VERSION) < 0
                ) {
                    logger.error(
                            "The server requested protocol version {}.{} but the currently installed client only " +
                            "supports {}.{}. Please switch to a different client version that supports the " +
                            "requested protocol.",
                            handshake.majorVersion, handshake.majorVersion,
                            PROTOCOL_MAJOR_VERSION, PROTOCOL_MINOR_VERSION
                    );
                    return this.disconnect();
                } else if(handshake.minorVersion.compareTo(PROTOCOL_MINOR_VERSION) > 0) {
                    logger.warn(
                            "The server requested protocol version {}.{} but the currently installed client only " +
                            "supports {}.{}. Functionality may be limited and bugs may arise, please switch to " +
                            "a different client version that supports the requested protocol.",
                            handshake.majorVersion, handshake.majorVersion,
                            PROTOCOL_MAJOR_VERSION, PROTOCOL_MINOR_VERSION
                    );
                }
                break;
            case ERROR_RESPONSE:
                var error = (ErrorResponse)packet;

                logger.error("{} - {}: {}", error.severity, error.errorCode, error.message);
                var exc = EdgeDBErrorException.fromError(error);

                if(!readyPromise.isDone()) {
                    readyPromise.completeExceptionally(exc);
                }
                return CompletableFuture.failedFuture(exc);
            case AUTHENTICATION:
                var auth = (AuthenticationStatus)packet;
                if(auth.authStatus == AuthStatus.AUTHENTICATION_REQUIRED_SASL_MESSAGE) {
                    return startSASLAuthentication(auth);
                } else if (auth.authStatus != AuthStatus.AUTHENTICATION_OK) {
                    throw new UnexpectedMessageException(
                            "Expected AuthenticationRequiredSASLMessage, got " + auth.authStatus
                    );
                }
                break;
            case SERVER_KEY_DATA:
                this.serverKey = new byte[32];
                ((ServerKeyData)packet).keyData.readBytes(this.serverKey);
                break;
            case STATE_DATA_DESCRIPTION:
                var stateDescriptor = (StateDataDescription)packet;

                var codec = CodecBuilder.getCodec(stateDescriptor.typeDescriptorId, Map.class);

                if(codec == null) {
                    assert stateDescriptor.typeDescriptorBuffer != null;

                    var reader = new PacketReader(stateDescriptor.typeDescriptorBuffer);
                    codec = CodecBuilder.buildCodec(this, stateDescriptor.typeDescriptorId, reader, Map.class);
                }

                this.stateCodec = codec;
                this.stateDescriptorId = stateDescriptor.typeDescriptorId;
                break;
            case PARAMETER_STATUS:
                // TODO: parameters
                parseServerSettings((ParameterStatus) packet);

                break;
            case LOG_MESSAGE:
                var msg = (LogMessage)packet;
                var formatted = msg.format();
                switch (msg.severity) {
                    case INFO:
                    case NOTICE:
                        logger.info(formatted);
                        break;
                    case DEBUG:
                        logger.debug(formatted);
                        break;
                    case WARNING:
                        logger.warn(formatted);
                        break;
                }
                break;
        }

        return CompletableFuture.completedFuture(null);
    }

    private void parseServerSettings(ParameterStatus status) throws EdgeDBException, OperationNotSupportedException {
        switch (status.name) {
            case "suggested_pool_concurrency":
                assert status.value != null;

                var buffer = new byte[status.value.readableBytes()];
                status.value.readBytes(buffer);
                var str = new String(buffer, StandardCharsets.UTF_8);

                try {
                    this.suggestedPoolConcurrency = Long.parseLong(str);
                } catch (NumberFormatException x) {
                    logger.error("suggested_pool_concurrency wasn't in a numeric format", x);
                }
                break;
            case "system_config":
                assert status.value != null;

                var reader = new PacketReader(status.value);
                var descriptorLength = reader.readInt32() - 16;
                var descriptorId = reader.readUUID();

                var codec = CodecBuilder.getCodec(descriptorId, Map.class);

                if(codec == null) {
                    var descriptorReader = new PacketReader(reader.readBytes(descriptorLength));
                    codec = CodecBuilder.buildCodec(this, descriptorId, descriptorReader, Map.class);

                    if(codec == null) {
                        throw new MissingCodecException("Failed to build codec for system_config");
                    }
                }

                reader.skip(BinaryProtocolUtils.INT_SIZE); // discard length

                //noinspection unchecked
                this.rawServerConfig = codec.deserialize(reader, codecContext);
                break;
        }
    }

    private CompletionStage<Void> startSASLAuthentication(AuthenticationStatus authStatus) throws ScramException {
        this.isIdle = false;

        final var scram = new Scram();

        assert authStatus.authenticationMethods != null;

        var method = authStatus.authenticationMethods[0];

        assert method != null;

        if(!method.equals("SCRAM-SHA-256")) {
            throw new ScramException("The only supported method is SCRAM-SHA-256, but the server wants " + method);
        }

        var connection = getConnectionArguments();
        var initialMessage = scram.buildInitialMessagePacket(connection.getUsername(), method);

        AtomicReference<byte[]> signature = new AtomicReference<>(new byte[0]);

        try{
            return this.duplexer.duplex(initialMessage, (state) -> {
                try {
                    switch (state.packet.getMessageType()) {
                        case AUTHENTICATION:
                            var auth = (AuthenticationStatus)state.packet;

                            switch (auth.authStatus) {
                                case AUTHENTICATION_SASL_CONTINUE:
                                    var result = scram.buildFinalMessage(auth, connection.getPassword());
                                    signature.set(result.signature);

                                    return this.duplexer.send(result.buildPacket());
                                case AUTHENTICATION_SASL_FINAL:
                                    var key = Scram.parseServerFinalMessage(auth);

                                    if(!Arrays.equals(signature.get(), key)) {
                                        logger.error(
                                                "The SCRAM signature didn't match. ours: {}, servers: {}.",
                                                signature.get(),
                                                key
                                        );
                                        throw new InvalidSignatureException();
                                    }
                                    break;
                                case AUTHENTICATION_OK:
                                    state.finishDuplexing();
                                    this.isIdle = false;
                                    break;
                                default:
                                    throw new UnexpectedMessageException(
                                            "Expected continue or final but got " + auth.authStatus
                                    );
                            }
                            break;
                        case ERROR_RESPONSE:
                            throw EdgeDBErrorException.fromError((ErrorResponse)state.packet);
                        default:
                            logger.error(
                                    "Unexpected message. expected: {} actual: {}",
                                    ServerMessageType.AUTHENTICATION,
                                    state.packet.getMessageType()
                            );
                            throw new CompletionException(
                                    new UnexpectedMessageException(
                                            ServerMessageType.AUTHENTICATION,
                                            state.packet.getMessageType()
                                    )
                            );
                    }
                } // TODO: should reconnect & should retry exceptions
                catch (Exception err) {
                    this.isIdle = false;
                    state.finishExceptionally(err);
                    return CompletableFuture.failedFuture(err);
                }

                return CompletableFuture.completedFuture(null);
            });
        }
        catch (Throwable x) {
            return CompletableFuture.failedFuture(x);
        }
    }

    @Override
    public CompletionStage<Void> connect() {
        return CompletableFuture
                .runAsync(() -> {
                    try {
                        this.connectionSemaphore.acquire();
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
        return this.duplexer.readNext()
                .thenCompose(packet -> {
                    if(packet == null) {
                        return CompletableFuture.failedFuture(new UnexpectedDisconnectException());
                    }

                    if(packet instanceof ReadyForCommand) {
                        this.readyPromise.complete(null);
                        return dispatchReady();
                    }

                    try {
                        return handlePacket(packet)
                                .thenCompose((v) -> doClientHandshake());
                    } catch (Throwable e) {
                        throw new CompletionException(e);
                    }
                });
    }

    @Override
    public CompletionStage<Void> disconnect() {
        return this.duplexer.disconnect();
    }

    private CompletionStage<Void> connectInternal() {
        if(this.isConnected()) {
            return CompletableFuture.completedFuture(null);
        }

        this.duplexer.reset();

        return retryableConnect()
                .thenApply(v -> getConnectionArguments())
                .thenApply(connection -> new ClientHandshake(
                        PROTOCOL_MAJOR_VERSION,
                        PROTOCOL_MINOR_VERSION,
                        new ConnectionParam[] {
                                new ConnectionParam("user", connection.getUsername()),
                                new ConnectionParam("database", connection.getDatabase())
                        },
                        new ProtocolExtension[0]
                ))
                .thenCompose(this.duplexer::send);
    }

    private CompletionStage<Void> retryableConnect() {
        try {
            return exceptionallyCompose(this.openConnection(), err -> {
                if(err instanceof ConnectionFailedTemporarilyException) {
                    if(this.connectionAttempts < getConfig().getMaxConnectionRetries()) {
                        this.connectionAttempts++;

                        logger.warn("Attempting to reconnect... {}/{}", this.connectionAttempts, getConfig().getMaxConnectionRetries(), err);

                        return disconnect()
                                .thenCompose(v -> retryableConnect());
                    } else {
                        logger.error("Failed to establish a connection after {} attempts", this.connectionAttempts, err);
                        this.connectionAttempts = 0;
                    }
                }

                return CompletableFuture.failedFuture(err);
            });
        }
        catch (Exception x) {
            return CompletableFuture.failedFuture(x);
        }
    }

    protected abstract void setTransactionState(TransactionState state);

    protected abstract CompletionStage<Void> openConnection()
            throws GeneralSecurityException, IOException, TimeoutException;
    protected abstract CompletionStage<Void> closeConnection();

    public final class ExecutionArguments {
        public final String query;
        public final Map<String, Object> args;
        public final Cardinality cardinality;
        public final EnumSet<Capabilities> capabilities;
        public final IOFormat format;
        public boolean isRetry;
        public final boolean implicitTypeNames;

        public Cardinality actualCardinality;
        public EnumSet<Capabilities> actualCapabilities;

        private @Nullable Long cacheKey;

        public CodecBuilder.QueryCodecs codecs;

        public final List<Data> data;

        private boolean completedParse;
        private byte parseAttempts;

        private boolean completedExecute;

        private boolean stateUpdated;
        private ByteBuf stateData;

        public ExecutionArguments(
                String query,
                Map<String, Object> args,
                Cardinality cardinality,
                EnumSet<Capabilities> capabilities,
                IOFormat format,
                boolean isRetry,
                boolean implicitTypeNames
        ) {
            this.data = new ArrayList<>();
            this.query = query;
            this.args = args;
            this.cardinality = cardinality;
            this.capabilities = capabilities;
            this.format = format;
            this.isRetry = isRetry;
            this.implicitTypeNames = implicitTypeNames;

            this.actualCardinality = cardinality;
            this.actualCapabilities = capabilities;
        }

        public ExecutionArguments deriveNew() {
            return new ExecutionArguments(
                    this.query,
                    this.args,
                    this.cardinality,
                    this.capabilities,
                    this.format,
                    this.isRetry,
                    this.implicitTypeNames
            );
        }

        public boolean isParseComplete() {
            return this.completedParse;
        }

        public boolean isExecuteComplete() {
            return this.completedExecute;
        }

        public void setStateData(ByteBuf state) {
            this.stateData = state;
        }

        public synchronized long getCacheKey() {
            if(cacheKey == null) {
                return cacheKey = CodecBuilder.getCacheKey(this.query, this.cardinality, this.format);
            }

            return cacheKey;
        }

        public EnumSet<CompilationFlags> getCompilationFlags() {
            var flags = EnumSet.of(CompilationFlags.NONE);

            if(getConfig().getImplicitTypeIds()) {
                flags.add(CompilationFlags.IMPLICIT_TYPE_IDS);
            }

            if(implicitTypeNames) {
                flags.add(CompilationFlags.IMPLICIT_TYPE_NAMES);
            }

            if(!getConfig().getExplicitObjectIds()) {
                flags.add(CompilationFlags.EXPLICIT_OBJECT_IDS);
            }

            return flags;
        }

        public long getImplicitLimit() {
            return getConfig().getImplicitLimit();
        }

        public Parse toParsePacket() {
            return new Parse(
                    capabilities,
                    getCompilationFlags(),
                    format,
                    cardinality,
                    query,
                    getImplicitLimit(),
                    stateDescriptorId,
                    stateData
            );
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public Execute toExecutePacket() throws OperationNotSupportedException, EdgeDBException {
            assert codecs != null;

            return new Execute(
                    capabilities,
                    getCompilationFlags(),
                    getImplicitLimit(),
                    format,
                    cardinality,
                    query,
                    stateDescriptorId,
                    stateData,
                    codecs.inputCodecId,
                    codecs.outputCodecId,
                    ArgumentCodec.serializeToBuffer((ArgumentCodec) codecs.inputCodec, args, codecContext)
            );
        }
    }
}
