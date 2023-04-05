package com.edgedb.examples;

import com.edgedb.driver.EdgeDBClient;
import com.edgedb.annotations.EdgeDBDeserializer;
import com.edgedb.annotations.EdgeDBName;
import com.edgedb.annotations.EdgeDBType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

public final class CustomDeserializer implements Example {
    private static final Logger logger = LoggerFactory.getLogger(CustomDeserializer.class);

    @EdgeDBType
    public static final class Person {
        private final String name;
        private final Long age;

        @EdgeDBDeserializer
        public Person(
                @EdgeDBName("name") String name,
                @EdgeDBName("age") Long age
        ) {
            this.name = name;
            this.age = age;

            logger.info("Custom deserializer called");
        }
    }

    @Override
    public CompletionStage<Void> run(EdgeDBClient client) {
        return client.execute("insert Person { name := 'Example', age := 123 } unless conflict on .name")
                .thenCompose(v -> client.queryRequiredSingle(Person.class, "select Person { name, age } filter .name = 'Example'"))
                .thenAccept(person -> logger.info("Person result: {person}"));
    }
}
