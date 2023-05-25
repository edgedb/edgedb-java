package com.edgedb.driver;

/**
 * Represents a collection of settings used when creating a transaction.
 */
public final class TransactionSettings {
    /**
     * Gets a builder used to construct a {@linkplain TransactionSettings}
     * @return A new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the default transaction settings.
     * @return A new {@linkplain TransactionSettings} initialized with default values.
     */
    public static TransactionSettings getDefault() {
        return new TransactionSettings();
    }

    TransactionSettings() { }

    private TransactionIsolation isolation = TransactionIsolation.SERIALIZABLE;
    private boolean isReadOnly;
    private boolean isDeferrable;
    private int retryAttempts = 3;

    /**
     * Gets the current isolation within the transaction.
     * @return A {@linkplain TransactionIsolation} representing the current transaction isolation mode.
     * @see TransactionIsolation
     */
    public TransactionIsolation getIsolation() {
        return isolation;
    }

    /**
     * Gets whether the transaction is read-only. Any data modifications with insert, update, or delete are disallowed.
     * Schema mutations via DDL are also disallowed.
     * @return {@code true} if the transaction is read-only; otherwise {@code false}.
     */
    public boolean isReadOnly() {
        return isReadOnly;
    }

    /**
     * Gets whether the transaction is deferrable. The transaction can be set to deferrable mode only when
     * the current isolation mode is {@linkplain TransactionIsolation#SERIALIZABLE} and
     * {@linkplain TransactionSettings#isReadOnly()} is {@code false}.
     * <br/><br/>
     * When all three of these properties are selected for a transaction, the transaction may block when first
     * acquiring its snapshot, after which it is able to run without the normal overhead of a
     * {@linkplain TransactionIsolation#SERIALIZABLE} transaction and without any risk of contributing to or being
     * canceled by a serialization failure. This mode is well suited for long-running reports or backups.
     * @return {@code true} if the transaction is deferrable; otherwise {@code false}.
     */
    public boolean isDeferrable() {
        return isDeferrable;
    }

    /**
     * Gets the number of attempts to retry the transaction before throwing. The default is {@code 3}.
     * @return The number of attempts to retry this transaction.
     */
    public int getRetryAttempts() {
        return retryAttempts;
    }

    /**
     * Represents a builder used to construct {@linkplain TransactionSettings}.
     * @see TransactionSettings
     */
    public static final class Builder {
        private TransactionIsolation isolation = TransactionIsolation.SERIALIZABLE;
        private boolean isReadOnly;
        private boolean isDeferrable;
        private int retryAttempts = 3;

        /**
         * Sets the current builders isolation settings.
         * @param isolation The isolation mode to use.
         * @return The current builder.
         * @see TransactionIsolation
         * @see TransactionSettings#getIsolation()
         */
        public Builder withIsolation(TransactionIsolation isolation) {
            this.isolation = isolation;
            return this;
        }

        /**
         * Sets whether the transaction is read-only. If {@code true}, any data modifications with insert, update, or
         * delete are disallowed. Schema mutations via DDL are also disallowed.
         * @param isReadOnly The value to set.
         * @return The current builder.
         * @see TransactionSettings#isReadOnly()
         */
        public Builder withReadOnly(boolean isReadOnly) {
            this.isReadOnly = isReadOnly;
            return this;
        }

        /**
         * Sets whether the transaction is deferrable. The transaction can be set to deferrable mode only when
         * the current isolation mode is {@linkplain TransactionIsolation#SERIALIZABLE} and
         * {@linkplain TransactionSettings#isReadOnly()} is {@code false}.
         * <br/><br/>
         * When all three of these properties are selected for a transaction, the transaction may block when first
         * acquiring its snapshot, after which it is able to run without the normal overhead of a
         * {@linkplain TransactionIsolation#SERIALIZABLE} transaction and without any risk of contributing to or being
         * canceled by a serialization failure. This mode is well suited for long-running reports or backups.
         * @param isDeferrable The value to set.
         * @return The current builder.
         */
        public Builder withDeferrable(boolean isDeferrable) {
            this.isDeferrable = isDeferrable;
            return this;
        }

        /**
         * Sets the number of attempts to retry the transaction before throwing. The default is {@code 3}.
         * @param retryAttempts The value to set.
         * @return The current builder.
         */
        public Builder withRetryAttempts(int retryAttempts) {
            this.retryAttempts = retryAttempts;
            return this;
        }

        /**
         * Constructs a new {@linkplain TransactionSettings} from this builder.
         * @return A {@linkplain TransactionSettings} with the values specified in this builder.
         */
        public TransactionSettings build() {
            TransactionSettings transactionSettings = new TransactionSettings();
            transactionSettings.retryAttempts = this.retryAttempts;
            transactionSettings.isolation = this.isolation;
            transactionSettings.isReadOnly = this.isReadOnly;
            transactionSettings.isDeferrable = this.isDeferrable;
            return transactionSettings;
        }
    }
}
