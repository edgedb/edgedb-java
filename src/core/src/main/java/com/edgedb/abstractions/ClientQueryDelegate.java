package com.edgedb.abstractions;

import com.edgedb.Capabilities;
import com.edgedb.EdgeDBQueryable;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface ClientQueryDelegate<T, U> {
    CompletionStage<U> run(EdgeDBQueryable client, Class<T> cls, String query, Map<String, Object> args, EnumSet<Capabilities> capabilities);
}
