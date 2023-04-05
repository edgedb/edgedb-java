package com.edgedb.examples;

import com.edgedb.driver.EdgeDBClient;
import com.edgedb.annotations.EdgeDBType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public final class JsonResults implements Example {
    private static final Logger logger = LoggerFactory.getLogger(JsonResults.class);
    private static final JsonMapper mapper = new JsonMapper();

    @EdgeDBType
    public static final class Person {
        public String name;
        public Long age;
    }

    @Override
    public CompletionStage<Void> run(EdgeDBClient client) {

        return client.queryJson("select Person { name, age }")
                .thenApply(result -> {
                    try {
                        return mapper.readValue(result.value, Person[].class);
                    } catch (JsonProcessingException e) {
                        throw new CompletionException(e);
                    }
                })
                .thenAccept(person -> logger.info("People deserialized from JSON: {}", (Object[]) person));
    }
}
