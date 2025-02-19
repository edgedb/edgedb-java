package com.gel.examples;

import com.gel.driver.GelClientPool;
import com.gel.driver.annotations.GelType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public final class JsonResults implements Example {
    private static final Logger logger = LoggerFactory.getLogger(JsonResults.class);
    private static final JsonMapper mapper = new JsonMapper();

    @GelType
    public static final class Person {
        public String name;
        public Long age;
    }

    @Override
    public CompletionStage<Void> run(GelClientPool clientPool) {

        return clientPool.queryJson("select Person { name, age }")
                .thenApply(result -> {
                    try {
                        return mapper.readValue(result.getValue(), Person[].class);
                    } catch (JsonProcessingException e) {
                        throw new CompletionException(e);
                    }
                })
                .thenAccept(person -> logger.info("People deserialized from JSON: {}", (Object[]) person));
    }
}