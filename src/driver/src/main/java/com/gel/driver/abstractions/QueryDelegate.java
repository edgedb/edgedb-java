package com.gel.driver.abstractions;

import com.gel.driver.Capabilities;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface QueryDelegate<T, U> {
    CompletionStage<U> run(Class<T> cls, String query, Map<String, Object> args, EnumSet<Capabilities> capabilities);
}