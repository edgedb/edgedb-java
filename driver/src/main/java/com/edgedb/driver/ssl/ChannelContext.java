package com.edgedb.driver.ssl;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class ChannelContext {
    public final long timeout;
    public final TimeUnit units;

    public final ByteBuffer clientAppData;

    public final Queue<ByteBuffer> serverAppDataBuffers;
    public final ChannelOperation operation;


    private ChannelState channelState;
    private final AsyncSSLChannel channel;

    public ChannelContext(AsyncSSLChannel channel, ChannelOperation operation, long timeout, TimeUnit units) {
        this.channel = channel;
        this.serverAppDataBuffers = new ArrayDeque<>();
        this.operation = operation;
        this.timeout = timeout;
        this.units = units;

        this.clientAppData = channel.getDataBuffer();
    }

    public ChannelContext(AsyncSSLChannel channel, ByteBuffer data, ChannelOperation operation, long timeout, TimeUnit units) {
        this.channel = channel;
        this.serverAppDataBuffers = new ArrayDeque<>();
        this.operation = operation;
        this.timeout = timeout;
        this.units = units;

        this.clientAppData = data;
    }

    public void setState(ChannelState state) {
        this.channelState = state;
    }

    public ChannelState getState() {
        return this.channelState;
    }
}

