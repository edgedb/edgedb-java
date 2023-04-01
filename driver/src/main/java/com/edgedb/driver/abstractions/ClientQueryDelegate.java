package com.edgedb.driver.abstractions;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface ClientQueryDelegate<T, U> {
    CompletionStage<U> run(EdgeDBQueryable client, Class<T> cls, String query, Map<String, Object> args, EnumSet<Capabilities> capabilities);
}
