package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.binary.duplexers.StreamDuplexer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class EdgeDBTCPClient extends EdgeDBBinaryClient {
    private SSLEngine engine;
    private SSLSocket socket;
    private AsynchronousSocketChannel channel;

    private SSLSession session;
    private ByteBuffer clientPacketBuffer;
    private ByteBuffer sslClientPacketBuffer;
    private ByteBuffer serverPacketBuffer;
    private ByteBuffer sslServerPacketBuffer;

    public EdgeDBTCPClient(EdgeDBConnection connection) {
        super(connection);
        setDuplexer(new StreamDuplexer(this));
    }

    @Override
    public CompletionStage<Void> openConnection() {
        // TODO: https://github.com/marianobarrios/tls-channel
        var connection = getConnection();

        return CompletableFuture
                .supplyAsync(() -> {
                    // construct our SSL engine
                    SSLEngine engine = null;

                    try {
                        engine = connection
                                .getSSLContext()
                                .createSSLEngine(connection.getHostname(), connection.getPort());
                    } catch (GeneralSecurityException | IOException e) {
                        return CompletableFuture.failedFuture(e);
                    }

                    engine.setUseClientMode(true);
                    this.engine = engine;

                    // create an asynchronous channel
                    AsynchronousSocketChannel channel = null;
                    try {
                        channel = AsynchronousSocketChannel.open();
                    } catch (IOException e) {
                        return CompletableFuture.failedFuture(e);
                    }

                    this.channel = channel;

                    return channel.connect(new InetSocketAddress(connection.getHostname(), connection.getPort()));
                })
                .thenAccept(future -> {
                    // wait for connection
                    try {
                        future.wait(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenRun(() -> {
                    // alloc buffers
                    this.session = this.engine.getSession();
                    this.clientPacketBuffer = ByteBuffer.allocate(this.session.getApplicationBufferSize());
                    this.sslClientPacketBuffer = ByteBuffer.allocate(this.session.getPacketBufferSize());
                    this.serverPacketBuffer = ByteBuffer.allocate(this.session.getApplicationBufferSize());
                    this.sslServerPacketBuffer = ByteBuffer.allocate(this.session.getPacketBufferSize());

                    engine.han
                });
    }

    @Override
    public CompletionStage<Void> closeConnection() {
        return null;
    }
}
