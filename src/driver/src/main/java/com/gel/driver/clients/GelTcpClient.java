package com.gel.driver.clients;

import com.gel.driver.*;
import com.gel.driver.async.ChannelCompletableFuture;
import com.gel.driver.binary.PacketSerializer;
import com.gel.driver.binary.duplexers.ChannelDuplexer;
import com.gel.driver.exceptions.ConnectionFailedTemporarilyException;
import com.gel.driver.util.SslUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNegotiator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSessionContext;

import static com.gel.driver.util.ComposableUtil.exceptionallyCompose;

public class GelTcpClient extends GelBinaryClient implements TransactableClient {
    private static final Logger logger = LoggerFactory.getLogger(GelTcpClient.class);
    private static final NioEventLoopGroup NETTY_TCP_GROUP = new NioEventLoopGroup();
    private static final EventExecutorGroup DUPLEXER_GROUP = new DefaultEventExecutorGroup(8);

    private final @NotNull ChannelDuplexer duplexer;
    private final Bootstrap bootstrap;
    private TransactionState transactionState;

    public GelTcpClient(GelConnection connection, GelClientConfig config, AutoCloseable poolHandle) {
        super(connection, config, poolHandle);
        this.duplexer = new ChannelDuplexer(this);

        this.bootstrap = new Bootstrap()
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .group(NETTY_TCP_GROUP)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(@NotNull SocketChannel ch) throws Exception {
                        var pipeline = ch.pipeline();

                        SslContext context = new SslContext() {
                            SslContext innerContext = SslUtils.applyTrustManager(
                                getConnectionArguments(), 
                                SslContextBuilder.forClient()
                                    .protocols("TLSv1.3")
                                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                                            ApplicationProtocolConfig.Protocol.ALPN,
                                            ApplicationProtocolConfig.SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                            "edgedb-binary"
                                    ))
                            ).build();

                            @Override
                            public boolean isClient() {
                                return innerContext.isClient();
                            }

                            @Override
                            public SSLSessionContext sessionContext() {
                                return innerContext.sessionContext();
                            }

                            @Override
                            public List<String> cipherSuites() {
                                return innerContext.cipherSuites();
                            }
                            
                            @Override
                            public ApplicationProtocolNegotiator applicationProtocolNegotiator() {
                                return innerContext.applicationProtocolNegotiator();
                            }

                            @Override
                            public SSLEngine newEngine(ByteBufAllocator byteBufAllocator) {
                                SSLEngine engine = innerContext.newEngine(byteBufAllocator);
                                SSLParameters params = engine.getSSLParameters();
                                if (getConnectionArguments().getTLSServerName() != null) {
                                    params.setServerNames(new ArrayList<>() {{
                                        add(new SNIHostName(
                                            getConnectionArguments().getTLSServerName()
                                        ));
                                    }});
                                }
                                engine.setSSLParameters(params);
                                return engine;
                            }
                            
                            @Override
                            public SSLEngine newEngine(ByteBufAllocator byteBufAllocator, String s, int i) {
                                SSLEngine engine = innerContext.newEngine(byteBufAllocator, s, i);
                                SSLParameters params = engine.getSSLParameters();
                                if (getConnectionArguments().getTLSServerName() != null) {
                                    params.setServerNames(new ArrayList<>() {{
                                        add(new SNIHostName(
                                            getConnectionArguments().getTLSServerName()
                                        ));
                                    }});
                                }
                                engine.setSSLParameters(params);
                                return engine;
                            }
                        };

                        pipeline.addLast(
                                "ssl",
                                context.newHandler(
                                    ch.alloc(),
                                    getConnectionArguments().getHostname(),
                                    getConnectionArguments().getPort()
                                )
                        );

                        // edgedb-binary protocol and duplexer
                        pipeline.addLast(
                                PacketSerializer.createDecoder(GelTcpClient.this),
                                PacketSerializer.createEncoder()
                        );

                        pipeline.addLast(DUPLEXER_GROUP, duplexer.channelHandler);

                        duplexer.init(ch);
                    }
                });
    }

    @Override
    public @NotNull ChannelDuplexer getDuplexer() {
        return this.duplexer;
    }

    @Override
    public void setTransactionState(TransactionState state) {
        this.transactionState = state;
    }

    @Override
    protected CompletionStage<Void> openConnection() {
        final var connection = getConnectionArguments();

        try {
            logger.debug("Opening connection from bootstrap");
            return exceptionallyCompose(
                    ChannelCompletableFuture.completeFrom(
                        bootstrap.connect(
                            connection.getHostname(),
                            connection.getPort()
                        )
                    ),  e -> {
                        logger.debug("Connection failed", e);

                        if(e instanceof CompletionException && e.getCause() instanceof ConnectException) {
                            return CompletableFuture.failedFuture(new ConnectionFailedTemporarilyException(e));
                        }

                        return CompletableFuture.failedFuture(e);
                    })
                    .orTimeout(
                        getConnectionArguments().getWaitUntilAvailable().value,
                        getConnectionArguments().getWaitUntilAvailable().unit
                    );
        }
        catch (Exception err) {
            logger.error("Failed to open connection", err);
            return CompletableFuture.failedFuture(err);
        }
    }

    @Override
    protected CompletionStage<Void> closeConnection() {
        return this.duplexer.disconnect();
    }

    @Override
    public TransactionState getTransactionState() {
        return this.transactionState;
    }

    @Override
    public CompletionStage<Void> startTransaction(@NotNull TransactionIsolation isolation, boolean readonly, boolean deferrable) {
        String query = "start transaction isolation " +
                isolation +
                ", " +
                (readonly ? "read only" : "read write") +
                ", " +
                (deferrable ? "" : "not ") +
                "deferrable";

        return execute(query, EnumSet.of(Capabilities.TRANSACTION));
    }

    @Override
    public CompletionStage<Void> commit() {
        return execute("commit", EnumSet.of(Capabilities.TRANSACTION));
    }

    @Override
    public CompletionStage<Void> rollback() {
        return execute("rollback", EnumSet.of(Capabilities.TRANSACTION));
    }
}
