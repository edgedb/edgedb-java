package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.binary.duplexers.SocketDuplexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.concurrent.*;

public class EdgeDBTCPClient extends EdgeDBBinaryClient {
    // placeholders
    private static final long WRITE_TIMEOUT = 5000;
    private static final long READ_TIMEOUT = 5000;
    private static final long CONNECTION_TIMEOUT = 5000;

    private static final Logger logger = LoggerFactory.getLogger(EdgeDBTCPClient.class);

    private final SocketDuplexer socketDuplexer;
    private SSLSocket socket;

    public EdgeDBTCPClient(EdgeDBConnection connection) throws IOException {
        super(connection);

        ExecutorService executor = Executors.newFixedThreadPool(5); // TODO: config option here

        socketDuplexer = new SocketDuplexer(this);
        setDuplexer(socketDuplexer);

    }

    @Override
    protected CompletionStage<Void> openConnectionAsync() {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        var connection = getConnection();

                        var context = connection.getSSLContext();

                        SSLSocketFactory factory = (SSLSocketFactory) context.getSocketFactory();

                        SSLSocket sslSocket = (SSLSocket) factory.createSocket("localhost", 10708);
                        SSLParameters sslp = sslSocket.getSSLParameters();

                        sslp.setApplicationProtocols(new String[] { "edgedb-binary" });

                        sslSocket.setSSLParameters(sslp);

                        sslSocket.startHandshake();

                        String ap = sslSocket.getApplicationProtocol();

                        return sslSocket;
                    }
                    catch (Exception x) {
                        throw new CompletionException(x);
                    }
                })
                .thenAccept((v) -> {
                    try {
                        this.socket = v;
                        this.socketDuplexer.init(v);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                });

    }

    @Override
    protected CompletionStage<Void> closeConnectionAsync() {
        return this.duplexer.disconnectAsync();
    }

}
