package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.binary.duplexers.ChannelDuplexer;
import com.edgedb.driver.ssl.AsyncSSLChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.*;

public class EdgeDBTCPClient extends EdgeDBBinaryClient {
    // placeholders
    private static final long WRITE_TIMEOUT = 5000;
    private static final long READ_TIMEOUT = 5000;
    private static final long CONNECTION_TIMEOUT = 5000;

    private static final Logger logger = LoggerFactory.getLogger(EdgeDBTCPClient.class);

    private final ChannelDuplexer channelDuplexer;
    private AsyncSSLChannel channel;

    public EdgeDBTCPClient(EdgeDBConnection connection) throws IOException {
        super(connection);

        ExecutorService executor = Executors.newFixedThreadPool(5); // TODO: config option here

        channelDuplexer = new ChannelDuplexer(this);
        setDuplexer(channelDuplexer);

    }

    @Override
    protected CompletionStage<Void> openConnectionAsync() {
        final var connection = getConnection();

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        var engine = connection.getSSLContext().createSSLEngine(connection.getHostname(), connection.getPort());

                        // TODO: config the pool.
                        return new AsyncSSLChannel(engine, ForkJoinPool.commonPool());
                    }
                    catch (Exception x) {
                        throw new CompletionException(x);
                    }
                })
                .thenCompose((asyncSSLChannel) -> {
                    try {
                        this.channel = asyncSSLChannel;

                        this.channel.setALPNProtocols("edgedb-binary");

                        return this.channel.connectAsync(connection.getHostname(), connection.getPort());
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                })
                .thenAccept((v) -> {
                    this.channelDuplexer.init(this.channel);
                });
    }

    @Override
    protected CompletionStage<Void> closeConnectionAsync() {
        return this.duplexer.disconnectAsync();
    }

}
