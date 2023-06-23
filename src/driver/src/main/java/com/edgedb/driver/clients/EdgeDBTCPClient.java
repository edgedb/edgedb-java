package com.edgedb.driver.clients;

import com.edgedb.driver.*;
import com.edgedb.driver.async.ChannelCompletableFuture;
import com.edgedb.driver.binary.PacketSerializer;
import com.edgedb.driver.binary.duplexers.ChannelDuplexer;
import com.edgedb.driver.exceptions.ConnectionFailedTemporarilyException;
import com.edgedb.driver.util.SslUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static com.edgedb.driver.util.ComposableUtil.exceptionallyCompose;

public class EdgeDBTCPClient extends EdgeDBBinaryClient implements TransactableClient {
    private static final Logger logger = LoggerFactory.getLogger(EdgeDBTCPClient.class);
    private static final NioEventLoopGroup NETTY_TCP_GROUP = new NioEventLoopGroup();
    private static final EventExecutorGroup DUPLEXER_GROUP = new DefaultEventExecutorGroup(8);

    private final @NotNull ChannelDuplexer duplexer;
    private final Bootstrap bootstrap;
    private TransactionState transactionState;

    public EdgeDBTCPClient(EdgeDBConnection connection, EdgeDBClientConfig config, AutoCloseable poolHandle) {
        super(connection, config, poolHandle);
        this.duplexer = new ChannelDuplexer(this);
        setDuplexer(this.duplexer);

        this.bootstrap = new Bootstrap()
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .group(NETTY_TCP_GROUP)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(@NotNull SocketChannel ch) throws Exception {
                        var pipeline = ch.pipeline();

                        var builder = SslContextBuilder.forClient()
                                .sslProvider(SslProvider.JDK)
                                .protocols("TLSv1.3")
                                .applicationProtocolConfig(new ApplicationProtocolConfig(
                                        ApplicationProtocolConfig.Protocol.ALPN,
                                        ApplicationProtocolConfig.SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                        "edgedb-binary"
                                ));

                        SslUtils.applyTrustManager(getConnectionArguments(), builder);

                        pipeline.addLast("ssl", builder.build().newHandler(ch.alloc()));

                        // edgedb-binary protocol and duplexer
                        pipeline.addLast(
                                PacketSerializer.createDecoder(),
                                PacketSerializer.createEncoder()
                        );

                        pipeline.addLast(DUPLEXER_GROUP, duplexer.channelHandler);

                        duplexer.init(ch);
                    }
                });
    }
    @Override
    protected void setTransactionState(TransactionState state) {
        this.transactionState = state;
    }

    @Override
    protected CompletionStage<Void> openConnection() {
        final var connection = getConnectionArguments();


        try {
            return exceptionallyCompose(
                    ChannelCompletableFuture.completeFrom(
                        bootstrap.connect(
                            connection.getHostname(),
                            connection.getPort()
                        )
                    ),
                        e -> {
                            if(e instanceof CompletionException && e.getCause() instanceof ConnectException) {
                                return CompletableFuture.failedFuture(new ConnectionFailedTemporarilyException(e));
                            }

                            return CompletableFuture.failedFuture(e);
                        }
                    )
                    .orTimeout(getConfig().getConnectionTimeoutValue(), getConfig().getConnectionTimeoutUnit());
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
