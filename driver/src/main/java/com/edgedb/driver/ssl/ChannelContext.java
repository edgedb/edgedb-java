package com.edgedb.driver.ssl;

import javax.net.ssl.SSLEngineResult;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class ChannelContext implements Closeable {
    public final long timeout;
    public final TimeUnit units;

    public final ByteBuffer clientAppData;
    public ByteBuffer clientNetData;

    public ByteBuffer peerNetData;

    public final Queue<ByteBuffer> serverAppDataBuffers;
    public final ChannelOperation operation;

    public final boolean externalAppData;

    private ChannelState channelState;
    private final AsyncSSLChannel channel;

    private ByteBuffer priorityAppDataBuffer;

    public ChannelContext(AsyncSSLChannel channel, ChannelOperation operation, long timeout, TimeUnit units) {
        this.channel = channel;
        this.serverAppDataBuffers = new ArrayDeque<>();
        this.operation = operation;
        this.timeout = timeout;
        this.units = units;

        this.clientAppData = channel.getAppDataBuffer();
        this.clientNetData = channel.getNetDataBuffer();
        this.peerNetData = channel.getNetDataBuffer();

        this.externalAppData = false;
    }

    public ChannelContext(AsyncSSLChannel channel, ByteBuffer data, ChannelOperation operation, long timeout, TimeUnit units) {
        this.channel = channel;
        this.serverAppDataBuffers = new ArrayDeque<>();
        this.operation = operation;
        this.timeout = timeout;
        this.units = units;

        this.clientAppData = data;
        this.externalAppData = true;

        this.clientNetData = channel.getNetDataBuffer();
        this.peerNetData = channel.getNetDataBuffer();
    }

    public void setState(ChannelState state) {
        this.channelState = state;
    }

    public ChannelState getState() {
        return this.channelState;
    }

    public ByteBuffer getAppDataBuffer() {
        if(this.priorityAppDataBuffer != null) {
            var buffer = this.priorityAppDataBuffer;
            this.priorityAppDataBuffer = null;
            return buffer;
        }

        return channel.getAppDataBuffer();
    }

    public ByteBuffer getNetDataBuffer() {
        return channel.getNetDataBuffer();
    }

    public void returnNetBuffer(ByteBuffer buffer) {
        channel.cacheNetBuffer(buffer);
    }
    public void returnAppBuffer(ByteBuffer buffer) {
        channel.cacheNetBuffer(buffer);
    }

    public SSLEngineResult.HandshakeStatus getHandshakeStatus(SSLEngineResult.HandshakeStatus defaultStatus) {
        switch (this.operation) {
            case READ:
                return SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
            case WRITE:
                return SSLEngineResult.HandshakeStatus.NEED_WRAP;
            default:
                return defaultStatus;
        }
    }

    protected void setPriorityAppDataBuffer(ByteBuffer buffer) {
        this.priorityAppDataBuffer = buffer;
    }

    @Override
    public void close()  {
        // return buffers
        if(!externalAppData) {
            channel.cacheAppBuffer(this.clientAppData);
        }


        channel.cacheNetBuffer(this.clientNetData);
    }
}

