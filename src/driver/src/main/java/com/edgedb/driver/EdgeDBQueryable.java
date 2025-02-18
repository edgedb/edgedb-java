package com.edgedb.driver;

import com.edgedb.driver.datatypes.Json;
import com.edgedb.driver.exceptions.ResultCardinalityMismatchException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Represents a generic EdgeDB queryable interface, providing methods to execute queries with cardinality control.
 * @see GelClientPool
 */
public interface EdgeDBQueryable {

    /**
     * Executes a query, ignoring the result.
     * @param query The query to execute.
     * @param args The optional map of arguments used within the query.
     * @param capabilities An enum set with the allowed capabilities of the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query.
     * @see Capabilities
     */
    CompletionStage<Void> execute(
            @NotNull String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities
    );

    /**
     * Executes a query, ignoring the result.
     * @param query The query to execute
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query.
     */
    default CompletionStage<Void> execute(@NotNull String query) {
        return execute(query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    /**
     * Executes a query, ignoring the result.
     * @param query The query to execute.
     * @param capabilities An enum set with the allowed capabilities of the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query.
     * @see Capabilities
     */
    default CompletionStage<Void> execute(@NotNull String query, @NotNull EnumSet<Capabilities> capabilities){
        return execute(query, null, capabilities);
    }

    /**
     * Executes a query, ignoring the result.
     * @param query The query to execute
     * @param args The optional map of arguments used within the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query.
     */
    default CompletionStage<Void> execute(@NotNull String query, @Nullable Map<String, Object> args){
        return execute(query, args, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    /**
     * Executes a query with the cardinality {@code MANY}.
     * @param cls The result type of the query.
     * @param query The query to execute.
     * @param args The optional map of arguments used within the query.
     * @param capabilities An enum set with the allowed capabilities of the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an immutable collection of {@linkplain T}.
     * @param <T> The result type of the query.
     * @see Capabilities
     */
    <T> CompletionStage<List<@Nullable T>> query(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    );

    /**
     * Executes a query with the cardinality {@code MANY}.
     * @param cls The result type of the query.
     * @param query The query to execute.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an immutable collection of {@linkplain T}.
     * @param <T> The result type of the query.
     */
    default <T> CompletionStage<List<@Nullable T>> query(@NotNull Class<T> cls, @NotNull String query) {
        return query(cls, query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    /**
     * Executes a query with the cardinality {@code MANY}.
     * @param cls The result type of the query.
     * @param query The query to execute.
     * @param capabilities An enum set with the allowed capabilities of the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an immutable collection of {@linkplain T}.
     * @param <T> The result type of the query.
     * @see Capabilities
     */
    default <T> CompletionStage<List<@Nullable T>> query(
            @NotNull Class<T> cls,
            @NotNull String query,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        return query(cls, query, null, capabilities);
    }

    /**
     * Executes a query with the cardinality {@code MANY}.
     * @param cls The result type of the query.
     * @param query The query to execute.
     * @param args The optional map of arguments used within the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an immutable collection of {@linkplain T}.
     * @param <T> The result type of the query.
     */
    default <T> CompletionStage<List<@Nullable T>> query(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args
    ) {
        return query(cls, query, args, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    /**
     * Executes a query with the cardinality {@code AT_MOST_ONE}.
     * @param cls The result type of the query.
     * @param query The query to execute.
     * @param args The optional map of arguments used within the query.
     * @param capabilities An enum set with the allowed capabilities of the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an instance of {@linkplain T} if the query has a result; otherwise
     * {@code null}.
     * @param <T> The result type of the query.
     * @see Capabilities
     */
    <T> CompletionStage<@Nullable T> querySingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    );

    /**
     * Executes a query with the cardinality {@code AT_MOST_ONE}.
     * @param cls The result type of the query.
     * @param query The query to execute.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an instance of {@linkplain T} if the query has a result; otherwise
     * {@code null}.
     * @param <T> The result type of the query.
     */
    default <T> CompletionStage<@Nullable T> querySingle(@NotNull Class<T> cls, @NotNull String query) {
        return querySingle(cls, query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    /**
     * Executes a query with the cardinality {@code AT_MOST_ONE}.
     * @param cls The result type of the query.
     * @param query The query to execute.
     * @param capabilities An enum set with the allowed capabilities of the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an instance of {@linkplain T} if the query has a result; otherwise
     * {@code null}.
     * @param <T> The result type of the query.
     * @see Capabilities
     */
    default <T> CompletionStage<@Nullable T> querySingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        return querySingle(cls, query, null, capabilities);
    }

    /**
     * Executes a query with the cardinality {@code AT_MOST_ONE}.
     * @param cls The result type of the query.
     * @param query The query to execute.
     * @param args The optional map of arguments used within the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an instance of {@linkplain T} if the query has a result; otherwise
     * {@code null}.
     * @param <T> The result type of the query.
     */
    default <T> CompletionStage<@Nullable T> querySingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args
    ) {
        return querySingle(cls, query, args, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    /**
     * Executes a query with the cardinality {@code ONE}
     * @param cls The result type of the query.
     * @param query The query to execute.
     * @param args The optional map of arguments used within the query.
     * @param capabilities An enum set with the allowed capabilities of the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an instance of {@linkplain T} if the query has a result; otherwise
     * a {@linkplain ResultCardinalityMismatchException} is raised.
     * @param <T> The result type of the query.
     * @see Capabilities
     */
    <T> CompletionStage<@NotNull T> queryRequiredSingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    );

    /**
     * Executes a query with the cardinality {@code ONE}
     * @param cls The result type of the query.
     * @param query The query to execute.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an instance of {@linkplain T} if the query has a result; otherwise
     * a {@linkplain ResultCardinalityMismatchException} is raised.
     * @param <T> The result type of the query.
     */
    default <T> CompletionStage<@NotNull T> queryRequiredSingle(@NotNull Class<T> cls, @NotNull String query) {
        return queryRequiredSingle(cls, query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    /**
     * Executes a query with the cardinality {@code ONE}
     * @param cls The result type of the query.
     * @param query The query to execute.
     * @param capabilities An enum set with the allowed capabilities of the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an instance of {@linkplain T} if the query has a result; otherwise
     * a {@linkplain ResultCardinalityMismatchException} is raised.
     * @param <T> The result type of the query.
     * @see Capabilities
     */
    default <T> CompletionStage<@NotNull T> queryRequiredSingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        return queryRequiredSingle(cls, query, null, capabilities);
    }

    /**
     * Executes a query with the cardinality {@code ONE}
     * @param cls The result type of the query.
     * @param query The query to execute.
     * @param args The optional map of arguments used within the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an instance of {@linkplain T} if the query has a result; otherwise
     * a {@linkplain ResultCardinalityMismatchException} is raised.
     * @param <T> The result type of the query.
     */
    default <T> CompletionStage<@NotNull T> queryRequiredSingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args
    ) {
        return queryRequiredSingle(cls, query, args, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    /**
     * Executes a query, specifying the result to be a single JSON array.
     * @param query The query to execute.
     * @param args The optional map of arguments used within the query.
     * @param capabilities An enum set with the allowed capabilities of the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is a single {@linkplain Json} dataclass, with the
     * {@linkplain Json#getValue()} being a JSON array.
     * @see Json
     * @see Capabilities
     */
    CompletionStage<@NotNull Json> queryJson(
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    );

    /**
     * Executes a query, specifying the result to be a single JSON array.
     * @param query The query to execute.
     * @param args The optional map of arguments used within the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is a single {@linkplain Json} dataclass, with the
     * {@linkplain Json#getValue()} being a JSON array.
     * @see Json
     */
    default CompletionStage<@NotNull Json> queryJson(@NotNull String query, @Nullable Map<String, Object> args) {
        return queryJson(query, args, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    /**
     * Executes a query, specifying the result to be a single JSON array.
     * @param query The query to execute.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is a single {@linkplain Json} dataclass, with the
     * {@linkplain Json#getValue()} being a JSON array.
     * @see Json
     */
    default CompletionStage<@NotNull Json> queryJson(@NotNull String query) {
        return queryJson(query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    /**
     * Executes a query, specifying the result to be a single JSON array.
     * @param query The query to execute.
     * @param capabilities An enum set with the allowed capabilities of the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is a single {@linkplain Json} dataclass, with the
     * {@linkplain Json#getValue()} being a JSON array.
     * @see Json
     * @see Capabilities
     */
    default CompletionStage<@NotNull Json> queryJson(
            @NotNull String query,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        return queryJson(query, null, capabilities);
    }

    /**
     * Executes a query, specifying the result to be a collection of JSON objects.
     * @param query The query to execute.
     * @param args The optional map of arguments used within the query.
     * @param capabilities An enum set with the allowed capabilities of the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an immutable collection of {@linkplain Json} dataclasses.
     * @see Json
     * @see Capabilities
     */
    CompletionStage<List<@NotNull Json>> queryJsonElements(
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    );

    /**
     * Executes a query, specifying the result to be a collection of JSON objects.
     * @param query The query to execute.
     * @param args The optional map of arguments used within the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an immutable collection of {@linkplain Json} dataclasses.
     * @see Json
     */
    default CompletionStage<List<@NotNull Json>> queryJsonElements(
            @NotNull String query,
            @Nullable Map<String, Object> args
    ) {
        return queryJsonElements(query, args, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    /**
     * Executes a query, specifying the result to be a collection of JSON objects.
     * @param query The query to execute.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an immutable collection of {@linkplain Json} dataclasses.
     * @see Json
     */
    default CompletionStage<List<@NotNull Json>> queryJsonElements(@NotNull String query) {
        return queryJsonElements(query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    /**
     * Executes a query, specifying the result to be a collection of JSON objects.
     * @param query The query to execute.
     * @param capabilities An enum set with the allowed capabilities of the query.
     * @return A {@linkplain CompletionStage} representing the asynchronous operation of executing the query. The result
     * of the {@linkplain CompletionStage} is an immutable collection of {@linkplain Json} dataclasses.
     * @see Json
     * @see Capabilities
     */
    default CompletionStage<List<@NotNull Json>> queryJsonElements(
            @NotNull String query,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        return queryJsonElements(query, null, capabilities);
    }
}
