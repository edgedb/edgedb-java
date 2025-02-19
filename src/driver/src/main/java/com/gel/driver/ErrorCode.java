package com.gel.driver;

import com.gel.driver.binary.BinaryEnum;
import org.jetbrains.annotations.NotNull;

/**
 * Represents different possible error codes returned by EdgeDB.
 * @see com.gel.driver.exceptions.GelErrorException
 */
public enum ErrorCode implements BinaryEnum<Integer> {
    INTERNAL_SERVER_ERROR                      (0x01_00_00_00),
    UNSUPPORTED_FEATURE_ERROR                  (0x02_00_00_00),
    PROTOCOL_ERROR                             (0x03_00_00_00),
    BINARY_PROTOCOL_ERROR                      (0x03_01_00_00),
    UNSUPPORTED_PROTOCOL_VERSION_ERROR         (0x03_01_00_01),
    TYPE_SPEC_NOT_FOUND_ERROR                  (0x03_01_00_02),
    UNEXPECTED_MESSAGE_ERROR                   (0x03_01_00_03),
    INPUT_DATA_ERROR                           (0x03_02_00_00),
    PARAMETER_TYPE_MISMATCH_ERROR              (0x03_02_01_00),
    STATE_MISMATCH_ERROR                       (0x03_02_02_00, false, true),
    RESULT_CARDINALITY_MISMATCH_ERROR          (0x03_03_00_00),
    CAPABILITY_ERROR                           (0x03_04_00_00),
    UNSUPPORTED_CAPABILITY_ERROR               (0x03_04_01_00),
    DISABLED_CAPABILITY_ERROR                  (0x03_04_02_00),
    QUERY_ERROR                                (0x04_00_00_00),
    INVALID_SYNTAX_ERROR                       (0x04_01_00_00),
    EDGE_Q_L_SYNTAX_ERROR                      (0x04_01_01_00),
    SCHEMA_SYNTAX_ERROR                        (0x04_01_02_00),
    GRAPH_Q_L_SYNTAX_ERROR                     (0x04_01_03_00),
    INVALID_TYPE_ERROR                         (0x04_02_00_00),
    INVALID_TARGET_ERROR                       (0x04_02_01_00),
    INVALID_LINK_TARGET_ERROR                  (0x04_02_01_01),
    INVALID_PROPERTY_TARGET_ERROR              (0x04_02_01_02),
    INVALID_REFERENCE_ERROR                    (0x04_03_00_00),
    UNKNOWN_MODULE_ERROR                       (0x04_03_00_01),
    UNKNOWN_LINK_ERROR                         (0x04_03_00_02),
    UNKNOWN_PROPERTY_ERROR                     (0x04_03_00_03),
    UNKNOWN_USER_ERROR                         (0x04_03_00_04),
    UNKNOWN_DATABASE_ERROR                     (0x04_03_00_05),
    UNKNOWN_PARAMETER_ERROR                    (0x04_03_00_06),
    SCHEMA_ERROR                               (0x04_04_00_00),
    SCHEMA_DEFINITION_ERROR                    (0x04_05_00_00),
    INVALID_DEFINITION_ERROR                   (0x04_05_01_00),
    INVALID_MODULE_DEFINITION_ERROR            (0x04_05_01_01),
    INVALID_LINK_DEFINITION_ERROR              (0x04_05_01_02),
    INVALID_PROPERTY_DEFINITION_ERROR          (0x04_05_01_03),
    INVALID_USER_DEFINITION_ERROR              (0x04_05_01_04),
    INVALID_DATABASE_DEFINITION_ERROR          (0x04_05_01_05),
    INVALID_OPERATOR_DEFINITION_ERROR          (0x04_05_01_06),
    INVALID_ALIAS_DEFINITION_ERROR             (0x04_05_01_07),
    INVALID_FUNCTION_DEFINITION_ERROR          (0x04_05_01_08),
    INVALID_CONSTRAINT_DEFINITION_ERROR        (0x04_05_01_09),
    INVALID_CAST_DEFINITION_ERROR              (0x04_05_01_0A),
    DUPLICATE_DEFINITION_ERROR                 (0x04_05_02_00),
    DUPLICATE_MODULE_DEFINITION_ERROR          (0x04_05_02_01),
    DUPLICATE_LINK_DEFINITION_ERROR            (0x04_05_02_02),
    DUPLICATE_PROPERTY_DEFINITION_ERROR        (0x04_05_02_03),
    DUPLICATE_USER_DEFINITION_ERROR            (0x04_05_02_04),
    DUPLICATE_DATABASE_DEFINITION_ERROR        (0x04_05_02_05),
    DUPLICATE_OPERATOR_DEFINITION_ERROR        (0x04_05_02_06),
    DUPLICATE_VIEW_DEFINITION_ERROR            (0x04_05_02_07),
    DUPLICATE_FUNCTION_DEFINITION_ERROR        (0x04_05_02_08),
    DUPLICATE_CONSTRAINT_DEFINITION_ERROR      (0x04_05_02_09),
    DUPLICATE_CAST_DEFINITION_ERROR            (0x04_05_02_0A),
    DUPLICATE_MIGRATION_ERROR                  (0x04_05_02_0B),
    SESSION_TIMEOUT_ERROR                      (0x04_06_00_00),
    IDLE_SESSION_TIMEOUT_ERROR                 (0x04_06_01_00, false, true),
    QUERY_TIMEOUT_ERROR                        (0x04_06_02_00),
    TRANSACTION_TIMEOUT_ERROR                  (0x04_06_0A_00),
    IDLE_TRANSACTION_TIMEOUT_ERROR             (0x04_06_0A_01),
    EXECUTION_ERROR                            (0x05_00_00_00),
    INVALID_VALUE_ERROR                        (0x05_01_00_00),
    DIVISION_BY_ZERO_ERROR                     (0x05_01_00_01),
    NUMERIC_OUT_OF_RANGE_ERROR                 (0x05_01_00_02),
    ACCESS_POLICY_ERROR                        (0x05_01_00_03),
    INTEGRITY_ERROR                            (0x05_02_00_00),
    CONSTRAINT_VIOLATION_ERROR                 (0x05_02_00_01),
    CARDINALITY_VIOLATION_ERROR                (0x05_02_00_02),
    MISSING_REQUIRED_ERROR                     (0x05_02_00_03),
    TRANSACTION_ERROR                          (0x05_03_00_00),
    TRANSACTION_CONFLICT_ERROR                 (0x05_03_01_00, false, true),
    TRANSACTION_SERIALIZATION_ERROR            (0x05_03_01_01),
    TRANSACTION_DEADLOCK_ERROR                 (0x05_03_01_02),
    WATCH_ERROR                                (0x05_04_00_00),
    CONFIGURATION_ERROR                        (0x06_00_00_00),
    ACCESS_ERROR                               (0x07_00_00_00),
    AUTHENTICATION_ERROR                       (0x07_01_00_00),
    AVAILABILITY_ERROR                         (0x08_00_00_00),
    BACKEND_UNAVAILABLE_ERROR                  (0x08_00_00_01, false, true),
    BACKEND_ERROR                              (0x09_00_00_00),
    UNSUPPORTED_BACKEND_FEATURE_ERROR          (0x09_00_01_00),
    LOG_MESSAGE                                (0xF0_00_00_00),
    WARNING_MESSAGE                            (0xF0_01_00_00);

    private final int code;
    private final boolean shouldRetry;
    private final boolean shouldReconnect;

    ErrorCode(int code) {
        this.code = code;
        this.shouldRetry = false;
        this.shouldReconnect = false;
    }

    ErrorCode(int code, boolean shouldReconnect, boolean shouldRetry) {
        this.code = code;
        this.shouldReconnect = shouldReconnect;
        this.shouldRetry = shouldRetry;
    }

    @Override
    public @NotNull Integer getValue() {
        return code;
    }

    public boolean shouldReconnect() {
        return shouldReconnect;
    }

    public boolean shouldRetry() {
        return shouldRetry;
    }
}
