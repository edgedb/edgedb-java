package com.edgedb.driver.binary.protocol.v1;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.ErrorCode;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.builders.CodecBuilder;
import com.edgedb.driver.binary.codecs.*;
import com.edgedb.driver.binary.codecs.scalars.TextCodec;
import com.edgedb.driver.binary.duplexers.Duplexer;
import com.edgedb.driver.binary.protocol.*;
import com.edgedb.driver.binary.protocol.common.*;
import com.edgedb.driver.binary.protocol.v1.descriptors.*;
import com.edgedb.driver.binary.protocol.v1.receivables.*;
import com.edgedb.driver.binary.protocol.v1.sendables.*;
import com.edgedb.driver.clients.GelBinaryClient;
import com.edgedb.driver.datatypes.Range;
import com.edgedb.driver.exceptions.*;
import com.edgedb.driver.util.BinaryProtocolUtils;
import com.edgedb.driver.util.Scram;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.edgedb.driver.util.ComposableUtil.composeWith;
import static com.edgedb.driver.util.ComposableUtil.exceptionallyCompose;
import static org.joou.Unsigned.ushort;

public class V1ProtocolProvider implements ProtocolProvider {
    private static final Logger logger = LoggerFactory.getLogger(V1ProtocolProvider.class);
    private static final Map<ServerMessageType, Function<@NotNull PacketReader, ? extends Receivable>> DESERIALIZER_MAP;
    private static final Map<DescriptorType, BiFunction<UUID, PacketReader, ? extends TypeDescriptor>> TYPE_DESCRIPTOR_MAP;
    private static final int MAX_PARSE_ATTEMPTS = 2;

    private static final Map<String, @Nullable Object> EMPTY_SERVER_CONFIG = Collections.emptyMap();

    static {
        DESERIALIZER_MAP = new HashMap<>(){{
            put(ServerMessageType.AUTHENTICATION, AuthenticationStatus::new);
            put(ServerMessageType.COMMAND_COMPLETE, CommandComplete::new);
            put(ServerMessageType.COMMAND_DATA_DESCRIPTION, CommandDataDescription::new);
            put(ServerMessageType.DATA, Data::new);
            put(ServerMessageType.DUMP_BLOCK, DumpBlock::new);
            put(ServerMessageType.DUMP_HEADER, DumpHeader::new);
            put(ServerMessageType.ERROR_RESPONSE, ErrorResponse::new);
            put(ServerMessageType.LOG_MESSAGE, LogMessage::new);
            put(ServerMessageType.PARAMETER_STATUS, ParameterStatus::new);
            put(ServerMessageType.READY_FOR_COMMAND, ReadyForCommand::new);
            put(ServerMessageType.RESTORE_READY, RestoreReady::new);
            put(ServerMessageType.SERVER_HANDSHAKE, ServerHandshake::new);
            put(ServerMessageType.SERVER_KEY_DATA, ServerKeyData::new);
            put(ServerMessageType.STATE_DATA_DESCRIPTION, StateDataDescription::new);
        }};

        TYPE_DESCRIPTOR_MAP = new HashMap<>(){{
            put(DescriptorType.ARRAY_TYPE_DESCRIPTOR,       ArrayTypeDescriptor::new);
            put(DescriptorType.BASE_SCALAR_TYPE_DESCRIPTOR, BaseScalarTypeDescriptor::new);
            put(DescriptorType.ENUMERATION_TYPE_DESCRIPTOR, EnumerationTypeDescriptor::new);
            put(DescriptorType.NAMED_TUPLE_DESCRIPTOR,      NamedTupleTypeDescriptor::new);
            put(DescriptorType.OBJECT_SHAPE_DESCRIPTOR,     ObjectShapeDescriptor::new);
            put(DescriptorType.SCALAR_TYPE_DESCRIPTOR,      ScalarTypeDescriptor::new);
            put(DescriptorType.SCALAR_TYPE_NAME_ANNOTATION, ScalarTypeNameAnnotation::new);
            put(DescriptorType.SET_DESCRIPTOR,              SetTypeDescriptor::new);
            put(DescriptorType.TUPLE_TYPE_DESCRIPTOR,       TupleTypeDescriptor::new);
            put(DescriptorType.INPUT_SHAPE_DESCRIPTOR,      InputShapeDescriptor::new);
            put(DescriptorType.RANGE_TYPE_DESCRIPTOR,       RangeTypeDescriptor::new);
        }};
    }

    private ProtocolPhase phase;
    private final GelBinaryClient client;
    private @Nullable Map<String, @Nullable Object> rawServerConfig;

    public V1ProtocolProvider(GelBinaryClient client) {
        this.client = client;

        this.phase = ProtocolPhase.CONNECTION;
    }

    @Override
    public ProtocolVersion getVersion() {
        return ProtocolVersion.of(1, 0);
    }

    @Override
    public ProtocolPhase getPhase() {
        return this.phase;
    }

    @Override
    public Map<String, @Nullable Object> getServerConfig() {
        return rawServerConfig == null ? EMPTY_SERVER_CONFIG : rawServerConfig;
    }

    @Override
    public Receivable readPacket(ServerMessageType type, int length, PacketReader reader) throws UnexpectedMessageException {
        if(!DESERIALIZER_MAP.containsKey(type)) {
            logger.error("Unknown packet type {}", type);
            reader.skip(length);
            throw new UnexpectedMessageException("Unsupported message type " + type);
        }

        return DESERIALIZER_MAP.get(type).apply(reader);
    }

    @Override
    public TypeDescriptorInfo<? extends Enum<?>> readDescriptor(PacketReader reader) throws UnexpectedMessageException {
        var type = reader.readEnum(DescriptorType.class, Byte.TYPE);
        var id = reader.readUUID();

        if(!TYPE_DESCRIPTOR_MAP.containsKey(type)) {
            logger.error("Unknown type descriptor {}", type);
            throw new UnexpectedMessageException("Unsupported descriptor type " + type);
        }

        return new TypeDescriptorInfo<>(TYPE_DESCRIPTOR_MAP.get(type).apply(id, reader), type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T>> @Nullable Codec<?> buildCodec(
            TypeDescriptorInfo<T> descriptor, Function<Integer, Codec<?>> getRelativeCodec,
            Function<Integer, TypeDescriptorInfo<?>> getRelativeDescriptor
    ) throws MissingCodecException {
        if(!(descriptor.type instanceof DescriptorType)) {
            throw new IllegalArgumentException("Expected v1 descriptor type, got " + descriptor.type.getClass().getName());
        }

        switch ((DescriptorType)descriptor.type) {
            case ARRAY_TYPE_DESCRIPTOR:
                var arrayType = descriptor.as(ArrayTypeDescriptor.class);

                return CodecBuilder.getOrCreateCodec(this, descriptor.getId(), null, (id, metadata) ->
                        new CompilableCodec(
                                id,
                                metadata,
                                getRelativeCodec.apply(arrayType.typePosition.intValue()),
                                ArrayCodec::new,
                                t -> Array.newInstance(t,0).getClass()
                        )
                );
            case SCALAR_TYPE_DESCRIPTOR:
            case BASE_SCALAR_TYPE_DESCRIPTOR:
                // should be resolved by the above case, getting here is an error
                throw new MissingCodecException(String.format("Could not find the scalar type %s", descriptor.getId().toString()));
            case ENUMERATION_TYPE_DESCRIPTOR:
                return CodecBuilder.getOrCreateCodec(this, descriptor.getId(), null, TextCodec::new);
            case INPUT_SHAPE_DESCRIPTOR:
                var inputShape = descriptor.as(InputShapeDescriptor.class);
                var inputShapeCodecs = new Codec[inputShape.shapes.length];
                var inputShapeNames = new String[inputShape.shapes.length];

                for (int i = 0; i != inputShape.shapes.length; i++) {
                    inputShapeCodecs[i] = getRelativeCodec.apply(inputShape.shapes[i].typePosition.intValue());
                    inputShapeNames[i] = inputShape.shapes[i].name;
                }

                return CodecBuilder.getOrCreateCodec(
                        this,
                        descriptor.getId(),
                        null,
                        (id, metadata) -> new SparseObjectCodec(id, metadata, inputShapeCodecs, inputShapeNames)
                );
            case TUPLE_TYPE_DESCRIPTOR:
                var tupleType = descriptor.as(TupleTypeDescriptor.class);
                var innerCodecs = new Codec<?>[tupleType.elementTypeDescriptorPositions.length];

                for(int i = 0; i != innerCodecs.length; i++) {
                    innerCodecs[i] = getRelativeCodec.apply(tupleType.elementTypeDescriptorPositions[i].intValue());
                }

                return CodecBuilder.getOrCreateCodec(
                        this,
                        descriptor.getId(),
                        null,
                        (id, metadata) -> new TupleCodec(id, metadata, innerCodecs)
                );
            case NAMED_TUPLE_DESCRIPTOR:
            {
                var tupleShape = descriptor.as(NamedTupleTypeDescriptor.class);

                var elements = new ObjectCodec.ObjectProperty[tupleShape.elements.length];

                for(var i = 0; i != tupleShape.elements.length; i++) {
                    var element = tupleShape.elements[i];
                    elements[i] = new ObjectCodec.ObjectProperty(
                            element.name,
                            getRelativeCodec.apply((int)element.typePosition),
                            null
                    );
                }

                return CodecBuilder.getOrCreateCodec(
                        this,
                        descriptor.getId(),
                        null,
                        (id, metadata) -> new ObjectCodec(id, null, metadata, elements)
                );
            }
            case OBJECT_SHAPE_DESCRIPTOR:
            {
                var objectShape = descriptor.as(ObjectShapeDescriptor.class);

                var elements = new ObjectCodec.ObjectProperty[objectShape.shapes.length];

                for(var i = 0; i != objectShape.shapes.length; i++) {
                    var element = objectShape.shapes[i];
                    elements[i] = new ObjectCodec.ObjectProperty(
                            element.name,
                            getRelativeCodec.apply(element.typePosition.intValue()),
                            element.cardinality
                    );
                }

                return CodecBuilder.getOrCreateCodec(
                        this,
                        descriptor.getId(),
                        null,
                        (id, metadata) -> new ObjectCodec(id, null, metadata, elements)
                );
            }
            case RANGE_TYPE_DESCRIPTOR:
                var rangeType = descriptor.as(RangeTypeDescriptor.class);

                return CodecBuilder.getOrCreateCodec(this, descriptor.getId(), null, (id, metadata) ->
                        new CompilableCodec(
                                id,
                                metadata,
                                getRelativeCodec.apply(rangeType.typePosition.intValue()),
                                RangeCodec::new,
                                t -> Range.empty(t).getClass()
                        )
                );
            case SCALAR_TYPE_NAME_ANNOTATION:
                return null;
            case SET_DESCRIPTOR:
                var setTypes = descriptor.as(SetTypeDescriptor.class);

                return CodecBuilder.getOrCreateCodec(this, descriptor.getId(), null, (id, metadata) ->
                        new CompilableCodec(
                                id,
                                metadata,
                                getRelativeCodec.apply(setTypes.typePosition.intValue()),
                                SetCodec::new,
                                t -> Array.newInstance(t, 0).getClass()
                        )
                );
            default:
                throw new MissingCodecException(String.format("Could not find a type descriptor with the type %s", descriptor.getId().toString()));
        }
    }

    public static class ProtocolState {
        public int attempts;
        public ByteBuf stateBuffer;

        public EnumSet<Capabilities> capabilities;
        public Cardinality cardinality;

        public boolean stateUpdated;

        public CodecBuilder.QueryCodecs codecs;

        public boolean isComplete;

        public List<ByteBuf> data;

        public ProtocolState(QueryParameters args, ByteBuf stateBuffer) {
            this.stateBuffer = stateBuffer;
            this.capabilities = args.capabilities;
            this.cardinality = args.cardinality;
        }

        public ProtocolState(QueryParameters args, ByteBuf stateBuffer, List<ByteBuf> data) {
            this(args, stateBuffer);
            this.data = data;
        }
    }

    @Override
    public CompletionStage<ParseResult> parseQuery(QueryParameters queryParameters) {
        ByteBuf stateBuffer;

        try {
            stateBuffer = client.serializeState();
        } catch (OperationNotSupportedException | EdgeDBException e) {
            return CompletableFuture.failedFuture(e);
        }

        if(queryParameters.format == IOFormat.NONE && (queryParameters.arguments == null || queryParameters.arguments.isEmpty())) {
            return CompletableFuture.completedFuture(new ParseResult(
                    CodecBuilder.NULL_CODEC,
                    CodecBuilder.NULL_CODEC,
                    CodecBuilder.NULL_CODEC_ID,
                    CodecBuilder.NULL_CODEC_ID,
                    stateBuffer,
                    queryParameters.capabilities,
                    queryParameters.cardinality
            ));
        }

        var cacheKey = queryParameters.getCacheKey();

        var cachedCodecs = CodecBuilder.getCachedCodecs(this, cacheKey);

        if(cachedCodecs == null) {
            ProtocolState parseState = new ProtocolState(queryParameters, stateBuffer);

            return runWithAttempts(
                    queryParameters,
                    a -> parse0(a, parseState),
                    ignored -> parseState.isComplete,
                    () -> parseState.attempts++
            ).thenApply(v ->
                new ParseResult(
                        parseState.codecs.inputCodec, parseState.codecs.outputCodec, parseState.codecs.inputCodecId,
                        parseState.codecs.outputCodecId, parseState.stateBuffer, parseState.capabilities,
                        parseState.cardinality
                )
            );
        }

        return CompletableFuture.completedFuture(
                new ParseResult(
                        cachedCodecs.inputCodec,
                        cachedCodecs.outputCodec,
                        cachedCodecs.inputCodecId,
                        cachedCodecs.outputCodecId,
                        stateBuffer,
                        queryParameters.capabilities,
                        queryParameters.cardinality
                )
        );
    }

    private CompletionStage<Void> parse0(@NotNull QueryParameters args, ProtocolState state) {
        if(state.attempts > MAX_PARSE_ATTEMPTS) {
            logger.debug("Parse attempts exceeded {}", MAX_PARSE_ATTEMPTS);
            return CompletableFuture.failedFuture(
                    new EdgeDBException("Failed to parse query after " + state.attempts + " attempts")
            );
        }

        var parseCardinality = state.cardinality;
        var parseCapabilities = state.capabilities;
        var stateBuffer = state.stateBuffer;

        logger.debug("Starting to parse... attempt {}/{}", state.attempts + 1, MAX_PARSE_ATTEMPTS);

        return client.getDuplexer().duplexAndSync(new Parse(
                parseCapabilities,
                getCompilationFlags(args),
                args.format,
                parseCardinality,
                args.query,
                client.getConfig().getImplicitLimit(),
                client.getStateDescriptorId(),
                stateBuffer
        ), (result) -> {
            logger.trace("parse duplex result: {}", result.packet.getMessageType());
            switch (result.packet.getMessageType()) {
                case ERROR_RESPONSE:
                    var err = result.packet.as(ErrorResponse.class);
                    logger.debug("handling error: {}", err.errorCode);
                    handleCommandError(args, state, result, err);
                    return CompletableFuture.completedFuture(null);
                case COMMAND_DATA_DESCRIPTION:
                    var commandDescriptor = result.packet.as(CommandDataDescription.class);

                    logger.debug("parsing command data description");

                    if(!Objects.equals(args.capabilities, commandDescriptor.capabilities)) {
                        logger.debug(
                                "actual capabilities differ from the provided ones: provided: {}. actual: {}",
                                args.capabilities,
                                commandDescriptor.capabilities
                        );

                        state.capabilities = commandDescriptor.capabilities;
                    }

                    if(args.cardinality != commandDescriptor.cardinality) {
                        logger.debug(
                                "actual cardinality differs from the provided one: provided: {}. actual: {}",
                                args.cardinality,
                                commandDescriptor.cardinality
                        );

                        state.cardinality = commandDescriptor.cardinality;
                    }

                    state.codecs = new CodecBuilder.QueryCodecs(
                            commandDescriptor.inputTypeDescriptorId,
                            CodecBuilder.buildCodec(
                                    client,
                                    commandDescriptor.inputTypeDescriptorId,
                                    commandDescriptor.inputTypeDescriptorBuffer
                            ),
                            commandDescriptor.outputTypeDescriptorId,
                            CodecBuilder.buildCodec(
                                    client,
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
                            this,
                            args.getCacheKey(),
                            commandDescriptor.inputTypeDescriptorId,
                            commandDescriptor.outputTypeDescriptorId
                    );
                    break;
                case STATE_DATA_DESCRIPTION:
                    updateStateCodec(state, result);
                    break;
                case READY_FOR_COMMAND:
                    var ready = result.packet.as(ReadyForCommand.class);
                    client.setTransactionState(ready.transactionState);
                    state.isComplete = state.codecs != null;
                    result.finishDuplexing();
            }

            return CompletableFuture.completedFuture(null);
        });
    }

    @Override
    public CompletionStage<ExecuteResult> executeQuery(QueryParameters queryParameters, ParseResult parseResult) {
        var data = new ArrayList<ByteBuf>();
        var state = new ProtocolState(queryParameters, parseResult.stateData, data);

        return runWithAttempts(
                queryParameters,
                p -> execute0(p, parseResult, state),
                p -> state.isComplete,
                () -> state.attempts++
        ).thenApply(v ->
                new ExecuteResult(
                        parseResult.outCodec,
                        data
                )
        );
    }

    private CompletionStage<Void> execute0(QueryParameters queryParameters, ParseResult parseResult, ProtocolState state) {
        if(state.attempts > 2) {
            return CompletableFuture.failedFuture(
                    new EdgeDBException("Failed to parse query after " + state.attempts + " attempts")
            );
        }

        if(!(parseResult.inCodec instanceof ArgumentCodec)) {
            return CompletableFuture.failedFuture(
                    new MissingCodecException(String.format(
                            "Cannot encode arguments, %s is not a valid argument codec",
                            parseResult.inCodec.toString())
                    )
            );
        }

        try {
            return client.getDuplexer().duplexAndSync(new Execute(
                    queryParameters.capabilities,
                    getCompilationFlags(queryParameters),
                    client.getConfig().getImplicitLimit(),
                    queryParameters.format,
                    queryParameters.cardinality,
                    queryParameters.query,
                    client.getStateDescriptorId(),
                    parseResult.stateData,
                    parseResult.inCodecId,
                    parseResult.outCodecId,
                    ArgumentCodec.serializeToBuffer(
                            (ArgumentCodec<?>) parseResult.inCodec,
                            queryParameters.arguments,
                            client.getCodecContext()
                    )
            ), (result) -> {
                switch (result.packet.getMessageType()) {
                    case DATA:
                        var data = result.packet.as(Data.class);
                        assert data.payloadBuffer != null;
                        // retain the data buffer once, so it's available for the
                        // consumer of data, since after this duplex step, `Data` and
                        // its children (buffers) are freed.
                        data.payloadBuffer.retain();
                        state.data.add(data.payloadBuffer);
                        break;
                    case STATE_DATA_DESCRIPTION:
                        updateStateCodec(state, result);
                        break;
                    case ERROR_RESPONSE:
                        var err = result.packet.as(ErrorResponse.class);
                        handleCommandError(queryParameters, state, result, err);
                        break;
                    case READY_FOR_COMMAND:
                        var ready = result.packet.as(ReadyForCommand.class);
                        client.setTransactionState(ready.transactionState);
                        state.isComplete = true;
                        result.finishDuplexing();
                        break;
                }

                return CompletableFuture.completedFuture(null);
            });
        } catch (OperationNotSupportedException | EdgeDBException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private @NotNull EnumSet<CompilationFlags> getCompilationFlags(QueryParameters args) {
        var flags = EnumSet.of(CompilationFlags.NONE);

        if(client.getConfig().getImplicitTypeIds()) {
            flags.add(CompilationFlags.IMPLICIT_TYPE_IDS);
        }

        if(args.implicitTypeNames) {
            flags.add(CompilationFlags.IMPLICIT_TYPE_NAMES);
        }

        if(!client.getConfig().getExplicitObjectIds()) {
            flags.add(CompilationFlags.EXPLICIT_OBJECT_IDS);
        }

        return flags;
    }

    private void handleCommandError(@NotNull QueryParameters queryParameters, @NotNull V1ProtocolProvider.ProtocolState args, Duplexer.@NotNull DuplexResult result, @NotNull ErrorResponse err) {
        logger.debug("Processing command phase error {}", err.errorCode);

        if(err.errorCode == ErrorCode.STATE_MISMATCH_ERROR) {
            logger.debug("Has updated state?: {}", args.stateUpdated);
            // should have new state
            if(!args.stateUpdated) {
                result.finishExceptionally(
                        "Failed to properly encode state data, this is a bug",
                        EdgeDBException::new
                );
            }
        }
        else {
            result.finishExceptionally(err, queryParameters.query, ErrorResponse::toException);
        }
    }

    private void updateStateCodec(ProtocolState state, Duplexer.@NotNull DuplexResult result) {
        var stateDescriptor = result.packet.as(StateDataDescription.class);
        var codec = CodecBuilder.getCodec(this, stateDescriptor.typeDescriptorId, Map.class);

        if(codec == null) {
            try {
                codec = CodecBuilder.buildCodec(
                        client,
                        stateDescriptor.typeDescriptorId,
                        stateDescriptor.typeDescriptorBuffer,
                        Map.class
                );
            } catch (EdgeDBException | OperationNotSupportedException e) {
                result.finishExceptionally("Failed to parse state codec", e, EdgeDBException::new);
            }
        }

        client.setStateCodec(codec);
        client.setStateDescriptorId(stateDescriptor.typeDescriptorId);

        state.stateUpdated = true;

        try {
            if(state.stateBuffer != null) {
                state.stateBuffer.release();
            }

            state.stateBuffer = client.serializeState();
        } catch (OperationNotSupportedException | EdgeDBException e) {
            result.finishExceptionally("Failed to serialize state", e, EdgeDBException::new);
        }
    }

    private CompletionStage<Void> runWithAttempts(
            QueryParameters args,
            @NotNull Function<QueryParameters, CompletionStage<Void>> delegate,
            @NotNull Predicate<QueryParameters> completedPredicate,
            @NotNull Runnable incrementer
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

    @Override
    public CompletionStage<Void> sendSyncMessage() {
        if(!client.getDuplexer().isConnected()) {
            return client.reconnect()
                    .thenCompose(v -> sendSyncMessage0(true));
        }

        return sendSyncMessage0(false);
    }

    private CompletionStage<Void> sendSyncMessage0(boolean isRetry) {
        return exceptionallyCompose(sendSyncMessage1(), e -> {
           if(!isRetry) {
               return sendSyncMessage0(true);
           }

           return CompletableFuture.failedFuture(new EdgeDBException("Failed to send sync message after 2 attempts"));
        });
    }

    private CompletionStage<Void> sendSyncMessage1() {
        return client.getDuplexer()
                .duplexSingle(sync())
                .thenAccept(r -> {
                    if(r == null) {
                        return;
                    }

                    if(r instanceof ErrorResponse) {
                        throw new CompletionException(((ErrorResponse)r).toException());
                    }

                    if(!(r instanceof ReadyForCommand)) {
                        throw new CompletionException(
                                new UnexpectedMessageException(
                                        ServerMessageType.READY_FOR_COMMAND,
                                        r.getMessageType()
                                )
                        );
                    }

                    client.setTransactionState(((ReadyForCommand)r).transactionState);

                });
    }

    @Override
    public CompletionStage<Void> processMessage(Receivable packet) {
        logger.debug("Processing packet {}", packet.getMessageType());

        try {
            switch (packet.getMessageType()) {
                case SERVER_HANDSHAKE:
                    var handshake = (ServerHandshake)packet;

                    if(!getVersion().equals(handshake.majorVersion, handshake.minorVersion)) {
                        var negotiated = client.tryNegotiateProtocol(handshake.majorVersion, handshake.minorVersion);

                        if(!negotiated && getVersion().major != handshake.majorVersion.intValue()) {
                            // major mismatch

                            logger.error(
                                "The server requested protocol version {}.{} but the currently installed client only " +
                                        "supports {}. Please switch to a different client version that supports the " +
                                        "requested protocol.",
                                handshake.majorVersion, handshake.majorVersion,
                                getVersion()
                            );

                            return client.disconnect();
                        } else if (!negotiated) {
                            // minor mismatch
                            logger.warn(
                                "The server requested protocol version {}.{} but the currently installed client only " +
                                        "supports {}. Functionality may be limited and bugs may arise, please switch to " +
                                        "a different client version that supports the requested protocol.",
                                handshake.majorVersion, handshake.majorVersion,
                                getVersion()
                            );
                        }

                        if(negotiated) {
                            // this providers lifecycle is complete.
                            return CompletableFuture.completedFuture(null);
                        }
                    }
                    break;
                case ERROR_RESPONSE:
                    var error = (ErrorResponse)packet;

                    logger.error("{} - {}: {}", error.severity, error.errorCode, error.message);

                    var exc = error.toException();

                    phase = ProtocolPhase.ERRORED;
                    client.cancelReadyState(exc);
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
                    // eventually used
                    byte[] serverKey = new byte[32];
                    ((ServerKeyData)packet).keyData.readBytes(serverKey);
                    break;
                case STATE_DATA_DESCRIPTION:
                    var stateDescriptor = (StateDataDescription)packet;

                    var codec = CodecBuilder.getCodec(this, stateDescriptor.typeDescriptorId, Map.class);

                    if(codec == null) {
                        assert stateDescriptor.typeDescriptorBuffer != null;

                        var reader = new PacketReader(stateDescriptor.typeDescriptorBuffer);
                        codec = CodecBuilder.buildCodec(client, stateDescriptor.typeDescriptorId, reader, Map.class);
                    }

                    client.setStateDescriptorId(stateDescriptor.typeDescriptorId);
                    client.setStateCodec(codec);
                    break;
                case PARAMETER_STATUS:
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
                case READY_FOR_COMMAND:
                    this.phase = ProtocolPhase.COMMAND;
                    break;
            }
        }
        catch (Exception x) {
            logger.debug("Processing failed", x);
            return CompletableFuture.failedFuture(x);
        }

        logger.debug("Processing pass-through to completed result");

        return CompletableFuture.completedFuture(null);
    }

    private void parseServerSettings(@NotNull ParameterStatus status) throws EdgeDBException, OperationNotSupportedException {
        switch (status.name) {
            case "suggested_pool_concurrency":
                assert status.value != null;

                var buffer = new byte[status.value.readableBytes()];
                status.value.readBytes(buffer);
                var str = new String(buffer, StandardCharsets.UTF_8);

                try {
                    client.setSuggestedPoolConcurrency(Long.parseLong(str));
                } catch (NumberFormatException x) {
                    logger.error("suggested_pool_concurrency wasn't in a numeric format", x);
                }
                break;
            case "system_config":
                assert status.value != null;

                var reader = new PacketReader(status.value);
                var descriptorLength = reader.readInt32() - 16;
                var descriptorId = reader.readUUID();

                var codec = CodecBuilder.getCodec(this, descriptorId, Map.class);

                if(codec == null) {
                    try(var descriptorReader = reader.scopedSlice(descriptorLength)) {
                        codec = CodecBuilder.buildCodec(client, descriptorId, descriptorReader, Map.class);
                    }
                } else {
                    reader.skip(descriptorLength);
                }

                reader.skip(BinaryProtocolUtils.INT_SIZE); // discard length

                //noinspection unchecked
                this.rawServerConfig = codec.deserialize(reader, client.getCodecContext());
                break;
        }
    }

    private CompletionStage<Void> startSASLAuthentication(@NotNull AuthenticationStatus authStatus) throws ScramException {
        this.phase = ProtocolPhase.AUTH;

        final var scram = new Scram();

        assert authStatus.authenticationMethods != null;

        var method = authStatus.authenticationMethods[0];

        assert method != null;

        if(!method.equals("SCRAM-SHA-256")) {
            throw new ScramException("The only supported method is SCRAM-SHA-256, but the server wants " + method);
        }

        var connection = client.getConnectionArguments();
        var initialMessage = scram.buildInitialMessage(connection.getUsername());

        AtomicReference<byte[]> signature = new AtomicReference<>(new byte[0]);



        try {
            return composeWith(
                    Unpooled.wrappedBuffer(initialMessage.getBytes(StandardCharsets.UTF_8)),
                    buffer -> client.getDuplexer().duplex(new AuthenticationSASLInitialResponse(
                            buffer,
                            method
                    ), (state) -> {
                        logger.debug("Authentication duplex: M:{}", state.packet.getMessageType());
                        try {
                            switch (state.packet.getMessageType()) {
                                case AUTHENTICATION:
                                    var auth = (AuthenticationStatus)state.packet;

                                    logger.debug("Processing auth part: {}", auth.authStatus);

                                    switch (auth.authStatus) {
                                        case AUTHENTICATION_SASL_CONTINUE:
                                            assert auth.saslData != null;

                                            var result = scram.buildFinalMessage(Scram.decodeString(auth.saslData), connection.getPassword());
                                            signature.set(result.signature);

                                            return composeWith(
                                                    Scram.encodeString(result.message),
                                                    resultBuffer -> client.getDuplexer().send(new AuthenticationSASLResponse(resultBuffer))
                                            );
                                        case AUTHENTICATION_SASL_FINAL:
                                            assert auth.saslData != null;

                                            var key = Scram.parseServerFinalMessage(auth.saslData);

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
                                            logger.debug("Completing auth duplex");
                                            state.finishDuplexing();
                                            break;
                                        default:
                                            throw new UnexpectedMessageException(
                                                    "Expected continue or final but got " + auth.authStatus
                                            );
                                    }
                                    break;
                                case ERROR_RESPONSE:
                                    throw ((ErrorResponse)state.packet).toException();
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
                            this.phase = ProtocolPhase.ERRORED;
                            state.finishExceptionally(err);
                            return CompletableFuture.failedFuture(err);
                        }

                        return CompletableFuture.completedFuture(null);
                    })
            );
        }
        catch (Throwable x) {
            return CompletableFuture.failedFuture(x);
        }
    }


    @Override
    public Sendable handshake() {
        var connection = client.getConnectionArguments();

        var connectionParams = new ConnectionParam[] {
                new ConnectionParam("user", connection.getUsername()),
                new ConnectionParam("database", connection.getDatabase()),
                new ConnectionParam("secret_key", connection.getSecretKey()),
                new ConnectionParam("branch", connection.getBranch())
        };

        return new ClientHandshake(
                ushort(getVersion().major),
                ushort(getVersion().minor),
                connectionParams,
                new ProtocolExtension[0]
        );
    }

    @Override
    public Sendable terminate() {
        return Terminate.INSTANCE;
    }

    @Override
    public Sendable sync() {
        return Sync.INSTANCE;
    }
}
