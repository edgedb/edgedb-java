package com.edgedb.examples;

import com.edgedb.driver.GelClientPool;
import com.edgedb.driver.annotations.EdgeDBType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

public final class QueryResults implements Example {
    private static final Logger logger = LoggerFactory.getLogger(QueryResults.class);

    @EdgeDBType
    public static final class Person {
        public String name;
        public Long age;

        public void setName(String name) {
            this.name = name;
        }
        public void setAge(Long age) {
            this.age = age;
        }
    }

    @Override
    public CompletionStage<Void> run(GelClientPool clientPool) {
        return clientPool.execute("insert Person { name := 'Example', age := 1234 } unless conflict on .name")
                .thenCompose(v -> clientPool.queryRequiredSingle(Person.class, "select Person { name, age } filter .name = 'Example'"))
                .thenAccept(result -> logger.info("Person returned from query: {}", result));
    }
}