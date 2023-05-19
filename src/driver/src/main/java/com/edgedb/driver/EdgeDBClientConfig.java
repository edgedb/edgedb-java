package com.edgedb.driver;

import com.edgedb.driver.namingstrategies.NamingStrategy;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class EdgeDBClientConfig {
    private int poolSize = 50;
    private ConnectionRetryMode retryMode;
    private int maxConnectionRetries = 5;
    private long connectionTimeout = 5;
    private TimeUnit connectionTimeoutUnit = TimeUnit.SECONDS;
    private long messageTimeout = 15;
    private TimeUnit messageTimeoutUnit = TimeUnit.SECONDS;
    private boolean explicitObjectIds;
    private long implicitLimit;
    private boolean implicitTypeIds;
    private NamingStrategy namingStrategy = NamingStrategy.defaultStrategy();
    private boolean useFieldSetters = false;
    private ClientType clientType = ClientType.TCP;
    private int clientAvailability = 10;
    private Duration clientMaxAge = Duration.of(10, ChronoUnit.MINUTES);

    public static EdgeDBClientConfig getDefault() {
        return new EdgeDBClientConfig();
    }

    public int getMaxConnectionRetries() {
        return maxConnectionRetries;
    }

    public void setMaxConnectionRetries(int maxConnectionRetries) {
        this.maxConnectionRetries = maxConnectionRetries;
    }

    public long getConnectionTimeout(@NotNull TimeUnit unit) {
        return unit.convert(connectionTimeout, this.connectionTimeoutUnit);
    }

    public void setConnectionTimeout(long connectionTimeout, @NotNull TimeUnit unit) {
        this.connectionTimeout = connectionTimeout;
        this.connectionTimeoutUnit = unit;
    }

    public long getMessageTimeout(@NotNull TimeUnit unit) {
        return unit.convert(this.messageTimeout, this.messageTimeoutUnit);
    }

    public void setMessageTimeout(long messageTimeout, @NotNull TimeUnit unit) {
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

    public void setConnectionRetryMode(@NotNull ConnectionRetryMode retryMode) {
        this.retryMode = retryMode;
    }

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(@NotNull NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public boolean useFieldSetters() {
        return useFieldSetters;
    }

    public void setUseFieldSetters(boolean useFieldSetters) {
        this.useFieldSetters = useFieldSetters;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(@NotNull ClientType clientType) {
        this.clientType = clientType;
    }

    public int getClientAvailability() {
        return clientAvailability;
    }

    public void setClientAvailability(int clientAvailability) {
        assert clientAvailability > 0;
        this.clientAvailability = clientAvailability;
    }

    public Duration getClientMaxAge() {
        return clientMaxAge;
    }

    public void setClientMaxAge(@NotNull Duration clientMaxAge) {
        this.clientMaxAge = clientMaxAge;
    }

    public boolean getImplicitTypeIds() {
        return implicitTypeIds;
    }

    public void setImplicitTypeIds(boolean implicitTypeIds) {
        this.implicitTypeIds = implicitTypeIds;
    }
}
