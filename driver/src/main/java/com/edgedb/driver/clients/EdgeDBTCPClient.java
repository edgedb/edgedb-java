package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.TLSSecurityMode;
import com.edgedb.driver.async.CompletableHandlerFuture;
import com.edgedb.driver.binary.duplexers.ChannelDuplexer;
import com.edgedb.driver.ssl.SSLAsynchronousChannelGroup;
import com.edgedb.driver.ssl.SSLAsynchronousSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.*;

public class EdgeDBTCPClient extends EdgeDBBinaryClient {
    // placeholders
    private static final long WRITE_TIMEOUT = 5000;
    private static final long READ_TIMEOUT = 5000;
    private static final long CONNECTION_TIMEOUT = 5000;

    private static final Logger logger = LoggerFactory.getLogger(EdgeDBTCPClient.class);

    private SSLAsynchronousSocketChannel channel;
    private final ExecutorService executor;

    private final ChannelDuplexer channelDuplexer;

    private static SSLAsynchronousChannelGroup channelGroup;

    public EdgeDBTCPClient(EdgeDBConnection connection) throws IOException {
        super(connection);

        this.executor = Executors.newFixedThreadPool(3); // TODO: config option here

        channelDuplexer = new ChannelDuplexer(this);
        setDuplexer(channelDuplexer);

        // TODO: config this
        if(channelGroup == null) {
            channelGroup = new SSLAsynchronousChannelGroup(executor);
        }
    }

    @Override
    protected CompletionStage<Void> openConnectionAsync() throws IOException {
        var connection = getConnection();

        this.channel = SSLAsynchronousSocketChannel.open(channelGroup, connection.getTLSSecurity() != TLSSecurityMode.INSECURE);

        var result = new CompletableHandlerFuture<Void, Void>();

        this.channel.connect(new InetSocketAddress(connection.getHostname(), connection.getPort()), null, result);

        return result.flatten().thenAccept((v) -> this.channelDuplexer.init(this.channel));
    }

    @Override
    protected CompletionStage<Void> closeConnectionAsync() {
        var result = new CompletableHandlerFuture<Void, Void>();

        this.channel.shutdown(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS, null, result);

        return result.flatten();
    }

}
