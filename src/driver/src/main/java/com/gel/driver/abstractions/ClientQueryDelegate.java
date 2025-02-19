package com.gel.driver.abstractions;

import com.gel.driver.Capabilities;
import com.gel.driver.GelQueryable;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface ClientQueryDelegate<T, U> {
    CompletionStage<U> run(GelQueryable client, Class<T> cls, String query, Map<String, Object> args, EnumSet<Capabilities> capabilities);
}
