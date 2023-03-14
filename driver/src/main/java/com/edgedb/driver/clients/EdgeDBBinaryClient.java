package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.async.AsyncSemaphore;
import com.edgedb.driver.binary.duplexers.Duplexer;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.receivable.*;
import com.edgedb.driver.binary.packets.sendables.ClientHandshake;
import com.edgedb.driver.binary.packets.shared.AuthStatus;
import com.edgedb.driver.binary.packets.shared.ConnectionParam;
import com.edgedb.driver.binary.packets.shared.ProtocolExtension;
import com.edgedb.driver.exceptions.*;
import com.edgedb.driver.util.Scram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("ClassEscapesDefinedScope")
public abstract class EdgeDBBinaryClient extends BaseEdgeDBClient {
    private static final Logger logger = LoggerFactory.getLogger(EdgeDBBinaryClient.class);

    private static final short PROTOCOL_MAJOR_VERSION = 1;
    private static final short PROTOCOL_MINOR_VERSION = 0;

    private byte[] serverKey;

    protected Duplexer duplexer;
    private boolean isIdle;
    private final AsyncSemaphore connectionSemaphore;
    private final CompletableFuture<Void> readyPromise;

    public EdgeDBBinaryClient(EdgeDBConnection connection) {
        super(connection);
        connectionSemaphore = new AsyncSemaphore(1);
        readyPromise = new CompletableFuture<>();
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

    @Override
    public CompletionStage<Void> executeAsync(String query, Hashtable<String, Object> args) {
        return null;
    }

    @Override
    public <T> CompletionStage<List<T>> queryAsync(String query, Hashtable<String, Object> args) {
        return null;
    }

    @Override
    public <T> CompletionStage<T> querySingleAsync(String query, Hashtable<String, Object> args) {
        return null;
    }

    @Override
    public <T> CompletionStage<T> queryRequiredSingleAsync(String query, Hashtable<String, Object> args) {
        return null;
    }

    private CompletionStage<Void> handlePacketAsync(Receivable packet) throws SSLException, ScramException, UnexpectedMessageException {
        switch (packet.getMessageType()) {
            case SERVER_HANDSHAKE:
                var handshake = (ServerHandshake)packet;

                if(handshake.majorVersion != PROTOCOL_MAJOR_VERSION || handshake.minorVersion < PROTOCOL_MINOR_VERSION) {
                    logger.error(
                            "The server requested protocol version {} but the currently installed client only supports {}. Please switch to a different client version that supports the requested protocol.",
                            String.format("%d.%d", handshake.majorVersion, handshake.majorVersion),
                            String.format("%d.%d", PROTOCOL_MAJOR_VERSION, PROTOCOL_MINOR_VERSION)
                    );
                    return this.disconnectAsync();
                } else if(handshake.minorVersion > PROTOCOL_MINOR_VERSION) {
                    logger.warn(
                            "The server requested protocol version {} but the currently installed client only supports {}. Functionality may be limited and bugs may arise, please switch to a different client version that supports the requested protocol.",
                            String.format("%d.%d", handshake.majorVersion, handshake.majorVersion),
                            String.format("%d.%d", PROTOCOL_MAJOR_VERSION, PROTOCOL_MINOR_VERSION)
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
                    return startSASLAuthenticationAsync(auth);
                } else if (auth.authStatus != AuthStatus.AUTHENTICATION_OK) {
                    throw new UnexpectedMessageException("Expected AuthenticationRequiredSASLMessage, got " + auth.authStatus);
                }
                break;
            case SERVER_KEY_DATA:
                this.serverKey = new byte[32];
                ((ServerKeyData)packet).keyData.readBytes(this.serverKey);
                break;
            case STATE_DATA_DESCRIPTION:
                // TODO: state
                break;
            case PARAMETER_STATUS:
                // TODO: parameters
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

    private CompletionStage<Void> startSASLAuthenticationAsync(AuthenticationStatus authStatus) throws ScramException, SSLException {
        this.isIdle = false;

        final var scram = new Scram();

        assert authStatus.authenticationMethods != null;

        var method = authStatus.authenticationMethods[0];

        if(method != "SCRAM-SHA-256") {
            throw new ScramException("The only supported method is SCRAM-SHA-256, but the server wants " + method);
        }

        var connection = getConnection();
        var initialMessage = scram.buildInitialMessagePacket(connection.getUsername(), method);

        AtomicReference<byte[]> signature = new AtomicReference<>(new byte[0]);

        return this.duplexer.duplexAsync(initialMessage, (state) -> {
            try {
                switch (state.packet.getMessageType()) {
                    case AUTHENTICATION:
                        var auth = (AuthenticationStatus)state.packet;

                        switch (auth.authStatus) {
                            case AUTHENTICATION_SASL_CONTINUE:
                                var result = scram.buildFinalMessage(auth, connection.getPassword());
                                signature.set(result.signature);

                                return this.duplexer.sendAsync(result.buildPacket());
                            case AUTHENTICATION_SASL_FINAL:
                                var key = Scram.parseServerFinalMessage(auth);

                                if(!Arrays.equals(signature.get(), key)) {
                                    logger.error("The SCRAM signature didn't match. ours: {}, servers: {}.", signature.get(), key);
                                    throw new InvalidSignatureException();
                                }
                                break;
                            case AUTHENTICATION_OK:
                                state.finishDuplexing();
                                this.isIdle = false;
                                break;
                            default:
                                throw new UnexpectedMessageException("Expected continue or final but got " + auth.authStatus);
                        }
                        break;
                    case ERROR_RESPONSE:
                        throw EdgeDBErrorException.fromError((ErrorResponse)state.packet);
                    default:
                        logger.error("Unexpected message. expected: {} actual: {}", ServerMessageType.AUTHENTICATION, state.packet.getMessageType());
                        throw new CompletionException(new UnexpectedMessageException(ServerMessageType.AUTHENTICATION, state.packet.getMessageType()));
                }
            } // TODO: should reconnect & should retry exceptions
            catch (Exception err) {
                this.isIdle = false;
                state.finishExceptionally(err);
            }

            return CompletableFuture.completedFuture(null);
        });
    }

    @Override
    public CompletionStage<Void> connectAsync() {
        return CompletableFuture
                .runAsync(this.connectionSemaphore::aquire)
                .thenCompose((v) -> this.connectInternalAsync())
                .thenRunAsync(this::doClientHandshake)
                .thenCompose((v) -> this.readyPromise);
    }

    private CompletionStage<Void> doClientHandshake() {
        return this.duplexer.readNextAsync()
                .thenCompose(packet -> {
                    if(packet == null) {
                        return CompletableFuture.failedFuture(new UnexpectedDisconnectException());
                    }

                    if(packet instanceof ReadyForCommand) {
                        this.readyPromise.complete(null);
                        return CompletableFuture.completedFuture(null);
                    }

                    try {
                        return handlePacketAsync(packet)
                                .thenCompose((v) -> doClientHandshake());
                    } catch (Throwable e) {
                        throw new CompletionException(e);
                    }
                });
    }

    @Override
    public CompletionStage<Void> disconnectAsync() {
        return null;
    }

    private CompletionStage<Void> connectInternalAsync() {
        if(this.getIsConnected()) {
            return CompletableFuture.completedFuture(null);
        }

        this.duplexer.reset();

        // TODO: handle socket errors proxied thru EdgeDBException when 'shouldReconnect' is true
        try {
            return this.openConnectionAsync()
                    .thenCompose((v) -> {
                        var connection = getConnection();
                        try {
                            return this.duplexer.sendAsync(new ClientHandshake(
                                    PROTOCOL_MAJOR_VERSION,
                                    PROTOCOL_MINOR_VERSION,
                                    new ConnectionParam[] {
                                            new ConnectionParam("user", connection.getUsername()),
                                            new ConnectionParam("database", connection.getDatabase())
                                    },
                                    new ProtocolExtension[0]
                            ));
                        } catch (SSLException e) {
                            // TODO: handle?
                            logger.warn("SSL error when sending packet", e);
                            throw new CompletionException(e);
                        }
                    });
        }
        catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    protected abstract CompletionStage<Void> openConnectionAsync() throws GeneralSecurityException, IOException, TimeoutException;
    protected abstract CompletionStage<Void> closeConnectionAsync();
}
