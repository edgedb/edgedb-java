package com.edgedb;

import com.edgedb.datatypes.Json;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public interface EdgeDBQueryable {
    CompletionStage<Void> execute(
            @NotNull String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities
    );
    default CompletionStage<Void> execute(@NotNull String query) {
        return execute(query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }
    default CompletionStage<Void> execute(@NotNull String query, @NotNull EnumSet<Capabilities> capabilities){
        return execute(query, null, capabilities);
    }
    default CompletionStage<Void> execute(@NotNull String query, @Nullable Map<String, Object> args){
        return execute(query, args, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    <T> CompletionStage<List<@Nullable T>> query(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    );
    default <T> CompletionStage<List<@Nullable T>> query(@NotNull Class<T> cls, @NotNull String query) {
        return query(cls, query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }
    default <T> CompletionStage<List<@Nullable T>> query(
            @NotNull Class<T> cls,
            @NotNull String query,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        return query(cls, query, null, capabilities);
    }
    default <T> CompletionStage<List<@Nullable T>> query(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args
    ) {
        return query(cls, query, args, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    <T> CompletionStage<@Nullable T> querySingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    );
    default <T> CompletionStage<@Nullable T> querySingle(@NotNull Class<T> cls, @NotNull String query) {
        return querySingle(cls, query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }
    default <T> CompletionStage<@Nullable T> querySingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        return querySingle(cls, query, null, capabilities);
    }
    default <T> CompletionStage<@Nullable T> querySingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args
    ) {
        return querySingle(cls, query, args, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    <T> CompletionStage<@NotNull T> queryRequiredSingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    );
    default <T> CompletionStage<@NotNull T> queryRequiredSingle(@NotNull Class<T> cls, @NotNull String query) {
        return queryRequiredSingle(cls, query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }
    default <T> CompletionStage<@NotNull T> queryRequiredSingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        return queryRequiredSingle(cls, query, null, capabilities);
    }
    default <T> CompletionStage<@NotNull T> queryRequiredSingle(
            @NotNull Class<T> cls,
            @NotNull String query,
            @Nullable Map<String, Object> args
    ) {
        return queryRequiredSingle(cls, query, args, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    CompletionStage<@NotNull Json> queryJson(
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    );
    default CompletionStage<@NotNull Json> queryJson(@NotNull String query, @Nullable Map<String, Object> args) {
        return queryJson(query, args, EnumSet.of(Capabilities.MODIFICATIONS));
    }
    default CompletionStage<@NotNull Json> queryJson(@NotNull String query) {
        return queryJson(query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }
    default CompletionStage<@NotNull Json> queryJson(
            @NotNull String query,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        return queryJson(query, null, capabilities);
    }

    CompletionStage<List<@NotNull Json>> queryJsonElements(
            @NotNull String query,
            @Nullable Map<String, Object> args,
            @NotNull EnumSet<Capabilities> capabilities
    );
    default CompletionStage<List<@NotNull Json>> queryJsonElements(
            @NotNull String query,
            @Nullable Map<String, Object> args
    ) {
        return queryJsonElements(query, args, EnumSet.of(Capabilities.MODIFICATIONS));
    }
    default CompletionStage<List<@NotNull Json>> queryJsonElements(@NotNull String query) {
        return queryJsonElements(query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }
    default CompletionStage<List<@NotNull Json>> queryJsonElements(
            @NotNull String query,
            @NotNull EnumSet<Capabilities> capabilities
    ) {
        return queryJsonElements(query, null, capabilities);
    }

}
