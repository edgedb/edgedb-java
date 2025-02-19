package com.gel.driver.state;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a session-level configuration.
 */
public final class Config {
    /**
     * The default configuration specified by the server
     */
    public static final Config DEFAULT = new Config();

    /**
     * Gets a new builder used to build a {@linkplain Config}.
     * @return A new builder instance.
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    private final @Nullable Duration idleTransactionTimeout;
    private final @Nullable Duration queryExecutionTimeout;
    private final @Nullable Boolean allowDMLInFunctions;
    private final @Nullable Boolean allowBareDDL;
    private final @Nullable Boolean applyAccessPolicies;

    /**
     * Constructs a new {@linkplain Config}.
     * @param idleTransactionTimeout The idle transaction timeout duration.
     * @param queryExecutionTimeout The query execution timeout duration.
     * @param allowDMLInFunctions Whether to allow data manipulations in EdgeQL functions.
     * @param allowBareDDL Whether to allow bare data definition queries.
     * @param applyAccessPolicies Whether to apply access policies.
     */
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

    /**
     * Constructs a new {@linkplain Config}.
     */
    public Config() {
        allowBareDDL = null;
        idleTransactionTimeout = null;
        queryExecutionTimeout = null;
        allowDMLInFunctions = null;
        applyAccessPolicies = null;
    }

    /**
     * Gets the idle transaction timeout of this session-level configuration.
     * @return A {@linkplain Duration} representing the idle transaction timeout of this session-level configuration. If
     * no value is present, {@code null} is returned.
     */
    public @Nullable Duration getIdleTransactionTimeout() {
        return idleTransactionTimeout;
    }

    /**
     * Gets the query execution timeout of this session-level configuration.
     * @return A {@linkplain Duration} representing the query execution timeout of this session-level configuration. If
     * no value is present, {@code null} is returned.
     */
    public @Nullable Duration getQueryExecutionTimeout() {
        return queryExecutionTimeout;
    }

    /**
     * Gets whether DML is allowed in EdgeQL functions.
     * @return {@code true} if DML is allowed in EdgeQL functions; otherwise {@code false}. If no value is present,
     * {@code null} is returned.
     */
    public @Nullable Boolean allowDMLInFunctions() {
        return allowDMLInFunctions;
    }

    /**
     * Gets whether DDL is allowed.
     * @return {@code true} if DDL is allowed; otherwise {@code false}. If no value is present, {@code null} is
     * returned.
     */
    public @Nullable Boolean allowBareDDL() {
        return allowBareDDL;
    }

    /**
     * Gets whether access policies are applied.
     * @return {@code true} if access policies are applied; otherwise {@code false}. If no value is present,
     * {@code null} is returned.
     */
    public @Nullable Boolean applyAccessPolicies() {
        return applyAccessPolicies;
    }

    /**
     * Serializes this config object to a sparse map.
     * @return A map containing the field values, if present.
     */
    public @NotNull Map<String, Object> serialize() {
        return new HashMap<>(){{
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
            }};
    }

    /**
     * Represents a builder used to construct a {@linkplain Config}.
     */
    public static final class Builder {
        private @Nullable Duration idleTransactionTimeout = DEFAULT.idleTransactionTimeout;
        private @Nullable Duration queryExecutionTimeout = DEFAULT.queryExecutionTimeout;
        private @Nullable Boolean allowDMLInFunctions = DEFAULT.allowDMLInFunctions;
        private @Nullable Boolean allowBareDDL = DEFAULT.allowBareDDL;
        private @Nullable Boolean applyAccessPolicies = DEFAULT.applyAccessPolicies;

        /**
         * Sets the current builders idle transaction timeout.
         * @param idleTransactionTimeout The new value.
         * @return The current builder.
         */
        public @NotNull Builder withIdleTransactionTimeout(@Nullable Duration idleTransactionTimeout) {
            this.idleTransactionTimeout = idleTransactionTimeout;
            return this;
        }

        /**
         * Sets the current builders query execution timeout.
         * @param queryExecutionTimeout The new value.
         * @return The current builder.
         */
        public @NotNull Builder withQueryExecutionTimeout(@Nullable Duration queryExecutionTimeout) {
            this.queryExecutionTimeout = queryExecutionTimeout;
            return this;
        }

        /**
         * Sets whether DML is allowed in EdgeQL functions.
         * @param allowDMLInFunctions The new value.
         * @return The current builder.
         */
        public @NotNull Builder allowDMLInFunctions(@Nullable Boolean allowDMLInFunctions) {
            this.allowDMLInFunctions = allowDMLInFunctions;
            return this;
        }

        /**
         * Sets whether bare DDL is allowed.
         * @param allowBareDDL The new value.
         * @return The current builder.
         */
        public @NotNull Builder allowBareDDL(@Nullable Boolean allowBareDDL) {
            this.allowBareDDL = allowBareDDL;
            return this;
        }

        /**
         * Sets whether access policies are applied.
         * @param applyAccessPolicies The new value.
         * @return The current builder.
         */
        public @NotNull Builder applyAccessPolicies(@Nullable Boolean applyAccessPolicies) {
            this.applyAccessPolicies = applyAccessPolicies;
            return this;
        }

        /**
         * Constructs a new {@linkplain Config} from this builder.
         * @return A {@linkplain Config} with the values specified in this builder.
         */
        public @NotNull Config build() {
            return new Config(idleTransactionTimeout, queryExecutionTimeout, allowDMLInFunctions, allowBareDDL, applyAccessPolicies);
        }
    }
}
