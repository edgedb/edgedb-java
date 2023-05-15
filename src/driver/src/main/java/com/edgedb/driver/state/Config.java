package com.edgedb.driver.state;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class Config {
    public static final Config DEFAULT = new Config();
    public static ConfigBuilder builder() {
        return new ConfigBuilder();
    }

    private final @Nullable Duration idleTransactionTimeout;
    private final @Nullable Duration queryExecutionTimeout;
    private final @Nullable Boolean allowDMLInFunctions;
    private final @Nullable Boolean allowBareDDL;

    public Config(
            @Nullable Duration idleTransactionTimeout,
            @Nullable Duration queryExecutionTimeout,
            @Nullable Boolean allowDMLInFunctions,
            @Nullable Boolean allowBareDDL,
            @Nullable Boolean applyAccessPolicies
    ) {
        this.idleTransactionTimeout = idleTransactionTimeout;
        this.queryExecutionTimeout = queryExecutionTimeout;
        this.allowDMLInFunctions = allowDMLInFunctions;
        this.allowBareDDL = allowBareDDL;
        this.applyAccessPolicies = applyAccessPolicies;
    }

    private final @Nullable Boolean applyAccessPolicies;

    public Config() {
        allowBareDDL = null;
        idleTransactionTimeout = null;
        queryExecutionTimeout = null;
        allowDMLInFunctions = null;
        applyAccessPolicies = null;
    }

    public Map<String, Object> serialize() {
        return new HashMap<>(){
            {
                if(idleTransactionTimeout != null) {
                    put("session_idle_transaction_timeout", idleTransactionTimeout);
                }

                if(queryExecutionTimeout != null) {
                    put("query_execution_timeout", queryExecutionTimeout);
                }

                if(allowDMLInFunctions != null) {
                    put("allow_dml_in_functions", allowDMLInFunctions);
                }

                if(allowBareDDL != null) {
                    put("allow_bare_ddl", allowBareDDL ? "AlwaysAllow" : "NeverAllow");
                }

                if(applyAccessPolicies != null) {
                    put("apply_access_policies", applyAccessPolicies);
                }
            }
        };
    }
}
