package com.edgedb.driver.state;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class Config {
    public static final Config DEFAULT = new Config();

    private final @Nullable Duration idleTransactionTimeout;
    private final @Nullable Duration queryExecutionTimeout;
    private final @Nullable Boolean allowDMLInFunctions;
    private final @Nullable Boolean allowBareDDL;
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
                    put("idle_transaction_timeout", idleTransactionTimeout);
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
