package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.binary.duplexers.Duplexer;
import com.edgedb.driver.binary.packets.ServerMessageType;
import com.edgedb.driver.binary.packets.receivable.AuthenticationStatus;
import com.edgedb.driver.binary.packets.receivable.ErrorResponse;
import com.edgedb.driver.binary.packets.receivable.Receivable;
import com.edgedb.driver.binary.packets.receivable.ServerHandshake;
import com.edgedb.driver.binary.packets.sendables.ClientHandshake;
import com.edgedb.driver.binary.packets.shared.ConnectionParam;
import com.edgedb.driver.binary.packets.shared.ProtocolExtension;
import com.edgedb.driver.exceptions.InvalidSignatureException;
import com.edgedb.driver.exceptions.ScramException;
import com.edgedb.driver.exceptions.UnexpectedMessageException;
import com.edgedb.driver.util.Scram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("ClassEscapesDefinedScope")
public abstract class EdgeDBBinaryClient extends BaseEdgeDBClient {
    private static Logger logger = LoggerFactory.getLogger(EdgeDBBinaryClient.class);

    private static final short PROTOCOL_MAJOR_VERSION = 1;
    private static final short PROTOCOL_MINOR_VERSION = 0;

    protected Duplexer duplexer;
    private boolean isIdle;
    private final Semaphore connectionSemaphore;
    private final CompletableFuture<Void> readyPromise;

    public EdgeDBBinaryClient(EdgeDBConnection connection) {
        super(connection);
        connectionSemaphore = new Semaphore(1);
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

    private CompletionStage<Void> handlePacketAsync(Receivable packet) {
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
                if(!readyPromise.isDone()) {
                    readyPromise.cancel(true); // TODO: complete with error?
                }
                break;
            case AUTHENTICATION:

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
                                break;
                            default:
                                throw new UnexpectedMessageException("Expected continue or final but got " + auth.authStatus);
                        }
                        break;
                    case ERROR_RESPONSE:

                        break;
                    default:
                        logger.error("Unexpected message. expected: {} actual: {}", ServerMessageType.AUTHENTICATION, state.packet.getMessageType());
                        throw new CompletionException(new UnexpectedMessageException(ServerMessageType.AUTHENTICATION, state.packet.getMessageType()));
                }
            }
            catch (Exception err) {
                state.finishExceptionally(err);
            }

            return CompletableFuture.completedFuture(null);
        });
    }

    @Override
    public CompletionStage<Void> connectAsync() {
        return CompletableFuture
                .runAsync(() -> {
                    try {
                        this.connectionSemaphore.acquire();
                    } catch (InterruptedException e) {
                        throw new CompletionException(e);
                    }
                })
                .thenCompose((v) -> {
                    try {
                        return this.connectInternalAsync();
                    } catch (Throwable e) {
                        throw new CompletionException(e);
                    }
                })
                .thenRunAsync(() -> {
                    while(!this.readyPromise.isDone()) {
                        ;
                    }
                })
                .thenCompose((v) -> this.readyPromise);
    }

    private CompletionStage<Void> doClientHandshake() {
        this.duplexer.readNextAsync()
                .thenCompose((result) -> {

                })
    }

    @Override
    public CompletionStage<Void> disconnectAsync() {
        return null;
    }

    private CompletionStage<Void> connectInternalAsync() throws GeneralSecurityException, IOException, TimeoutException {
        if(this.getIsConnected()) {
            return CompletableFuture.completedFuture(null);
        }

        this.duplexer.reset();

        // TODO: handle socket errors proxied thru EdgeDBException when 'shouldReconnect' is true
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

    protected abstract CompletionStage<Void> openConnectionAsync() throws GeneralSecurityException, IOException, TimeoutException;
    protected abstract CompletionStage<Void> closeConnectionAsync();
}
