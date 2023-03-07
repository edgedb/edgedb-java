package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.async.ChannelCompletableFuture;
import com.edgedb.driver.binary.PacketSerializer;
import com.edgedb.driver.binary.duplexers.ChannelDuplexer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class EdgeDBTCPClient extends EdgeDBBinaryClient {
    // placeholders
    private static final long WRITE_TIMEOUT = 5000;
    private static final long READ_TIMEOUT = 5000;
    private static final long CONNECTION_TIMEOUT = 5000;

    private static final Logger logger = LoggerFactory.getLogger(EdgeDBTCPClient.class);

    private final ChannelDuplexer duplexer;

    public EdgeDBTCPClient(EdgeDBConnection connection) throws IOException {
        super(connection);

        this.duplexer = new ChannelDuplexer(this);
        setDuplexer(this.duplexer);
    }

    @Override
    protected CompletionStage<Void> openConnectionAsync() {
        verifyReflectionModeForNetty();
        final var connection = getConnection();
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            var bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(@NotNull SocketChannel ch) throws Exception {
                            var pipeline = ch.pipeline();

                            // SSL
//                            var engine = connection.getSSLContext().createSSLEngine(connection.getHostname(), connection.getPort());
//                            engine.setUseClientMode(true);
//
//                            var params = engine.getSSLParameters();
//                            params.setApplicationProtocols(new String[] {"edgedb-binary"});
//                            engine.setSSLParameters(params);


                            var builder = SslContextBuilder.forClient()
                                    .sslProvider(SslProvider.OPENSSL)
                                    .protocols("TLSv1.3")
                                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                                            ApplicationProtocolConfig.Protocol.ALPN,
                                            ApplicationProtocolConfig.SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                            "edgedb-binary"
                                    ));

                            connection.applyTrustManager(builder);

                            pipeline.addLast("ssl", builder.build().newHandler(ch.alloc()));

                            // edgedb-binary protocol and duplexer
                            pipeline.addLast(
                                    PacketSerializer.PACKET_DECODER,
                                    PacketSerializer.PACKET_ENCODER
                            );

                            pipeline.addLast(duplexer.channelHandler);

                            duplexer.init(ch);
                        }
                    });

            return ChannelCompletableFuture.completeFrom(bootstrap.connect(connection.getHostname(), connection.getPort()));
        }
        catch (Exception err) {
            logger.error("Failed to open connection", err);
            return CompletableFuture.failedFuture(err);
        }
    }

    private static synchronized void verifyReflectionModeForNetty() {
        var v = System.getProperty("Dio.netty.tryReflectionSetAccessible");
        if(v == null || v.equals("false")){
            System.setProperty("Dio.netty.tryReflectionSetAccessible", "true");
        }
    }

    @Override
    protected CompletionStage<Void> closeConnectionAsync() {
        return this.duplexer.disconnectAsync();
    }

}
