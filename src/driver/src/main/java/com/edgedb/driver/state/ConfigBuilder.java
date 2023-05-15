package com.edgedb.driver.state;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public final class ConfigBuilder {
    private @Nullable Duration idleTransactionTimeout;
    private @Nullable Duration queryExecutionTimeout;
    private @Nullable Boolean allowDMLInFunctions;
    private @Nullable Boolean allowBareDDL;
    private @Nullable Boolean applyAccessPolicies;

    public ConfigBuilder withIdleTransactionTimeout(@Nullable Duration idleTransactionTimeout) {
        this.idleTransactionTimeout = idleTransactionTimeout;
        return this;
    }

    public ConfigBuilder withQueryExecutionTimeout(@Nullable Duration queryExecutionTimeout) {
        this.queryExecutionTimeout = queryExecutionTimeout;
        return this;
    }

    public ConfigBuilder withAllowDMLInFunctions(@Nullable Boolean allowDMLInFunctions) {
        this.allowDMLInFunctions = allowDMLInFunctions;
        return this;
    }

    public ConfigBuilder withAllowBareDDL(@Nullable Boolean allowBareDDL) {
        this.allowBareDDL = allowBareDDL;
        return this;
    }

    public ConfigBuilder withApplyAccessPolicies(@Nullable Boolean applyAccessPolicies) {
        this.applyAccessPolicies = applyAccessPolicies;
        return this;
    }

    public Config build() {
        return new Config(idleTransactionTimeout, queryExecutionTimeout, allowDMLInFunctions, allowBareDDL, applyAccessPolicies);
    }
}
