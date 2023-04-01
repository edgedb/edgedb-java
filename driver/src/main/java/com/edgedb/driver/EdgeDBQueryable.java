package com.edgedb.driver;

import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public interface EdgeDBQueryable {
    CompletionStage<Void> execute(
            String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities
    );
    default CompletionStage<Void> execute(String query) {
        return execute(query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }
    default CompletionStage<Void> execute(String query, EnumSet<Capabilities> capabilities){
        return execute(query, null, capabilities);
    }

    <T> CompletionStage<List<T>> query(
            Class<T> cls,
            String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities
    );
    default <T> CompletionStage<List<T>> query(Class<T> cls, String query) {
        return query(cls, query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }
    default <T> CompletionStage<List<T>> query(Class<T> cls, String query, EnumSet<Capabilities> capabilities) {
        return query(cls, query, null, capabilities);
    }

    <T> CompletionStage<T> querySingle(
            Class<T> cls,
            String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities
    );
    default <T> CompletionStage<T> querySingle(Class<T> cls, String query) {
        return querySingle(cls, query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    default <T> CompletionStage<T> querySingle(Class<T> cls, String query, EnumSet<Capabilities> capabilities) {
        return querySingle(cls, query, null, capabilities);
    }

    <T> CompletionStage<T> queryRequiredSingle(
            Class<T> cls,
            String query,
            @Nullable Map<String, Object> args,
            EnumSet<Capabilities> capabilities
    );
    default <T> CompletionStage<T> queryRequiredSingle(Class<T> cls, String query) {
        return queryRequiredSingle(cls, query, null, EnumSet.of(Capabilities.MODIFICATIONS));
    }

    default <T> CompletionStage<T> queryRequiredSingle(Class<T> cls, String query, EnumSet<Capabilities> capabilities) {
        return queryRequiredSingle(cls, query, null, capabilities);
    }
}
