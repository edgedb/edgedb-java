package com.gel.examples;

import com.gel.driver.GelClientPool;
import com.gel.driver.annotations.GelDeserializer;
import com.gel.driver.annotations.GelName;
import com.gel.driver.annotations.GelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

public final class CustomDeserializer implements Example {
    private static final Logger logger = LoggerFactory.getLogger(CustomDeserializer.class);

    @GelType
    public static final class Person {
        private final String name;
        private final Long age;

        @GelDeserializer
        public Person(
                @GelName("name") String name,
                @GelName("age") Long age
        ) {
            this.name = name;
            this.age = age;

            logger.info("Custom deserializer called");
        }
    }

    @Override
    public CompletionStage<Void> run(GelClientPool clientPool) {
        return clientPool.execute("insert Person { name := 'Example', age := 123 } unless conflict on .name")
                .thenCompose(v -> clientPool.queryRequiredSingle(Person.class, "select Person { name, age } filter .name = 'Example'"))
                .thenAccept(person -> logger.info("Person result: {person}"));
    }
}