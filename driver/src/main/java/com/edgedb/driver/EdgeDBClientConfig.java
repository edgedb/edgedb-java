package com.edgedb.driver;

import java.util.concurrent.TimeUnit;

public final class EdgeDBClientConfig {
    private int poolSize = 50;
    private ConnectionRetryMode retryMode;
    private int maxConnectionRetries = 5;
    private long connectionTimeout = 5;
    private TimeUnit connectionTimeoutUnit = TimeUnit.SECONDS;
    private long messageTimeout = 15;
    private TimeUnit messageTimeoutUnit = TimeUnit.SECONDS;
    private boolean explicitObjectIds;
    private long implicitLimit;

    public static EdgeDBClientConfig getDefault() {
        return new EdgeDBClientConfig();
    }

    public int getMaxConnectionRetries() {
        return maxConnectionRetries;
    }

    public void setMaxConnectionRetries(int maxConnectionRetries) {
        this.maxConnectionRetries = maxConnectionRetries;
    }

    public long getConnectionTimeout(TimeUnit unit) {
        return unit.convert(connectionTimeout, this.connectionTimeoutUnit);
    }

    public void setConnectionTimeout(long connectionTimeout, TimeUnit unit) {
        this.connectionTimeout = connectionTimeout;
        this.connectionTimeoutUnit = unit;
    }

    public long getMessageTimeout(TimeUnit unit) {
        return unit.convert(this.messageTimeout, this.messageTimeoutUnit);
    }

    public void setMessageTimeout(long messageTimeout, TimeUnit unit) {
        this.messageTimeout = messageTimeout;
        this.messageTimeoutUnit = unit;
    }

    public boolean getExplicitObjectIds() {
        return explicitObjectIds;
    }

    public void setExplicitObjectIds(boolean explicitObjectIds) {
        this.explicitObjectIds = explicitObjectIds;
    }

    public long getImplicitLimit() {
        return implicitLimit;
    }

    public void setImplicitLimit(long implicitLimit) {
        this.implicitLimit = implicitLimit;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        if(poolSize <=0 ) {
            throw new IllegalArgumentException("Pool size must be at least 1");
        }

        this.poolSize = poolSize;
    }

    public ConnectionRetryMode getConnectionRetryMode() {
        return retryMode;
    }

    public void setConnectionRetryMode(ConnectionRetryMode retryMode) {
        this.retryMode = retryMode;
    }
}
