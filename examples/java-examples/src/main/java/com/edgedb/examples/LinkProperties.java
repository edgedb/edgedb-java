package com.edgedb.examples;

import com.edgedb.driver.GelClientPool;
import com.edgedb.driver.annotations.GelLinkType;
import com.edgedb.driver.annotations.GelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.CompletionStage;

public final class LinkProperties implements Example {
    private static final Logger logger = LoggerFactory.getLogger(LinkProperties.class);

    @GelType
    public static final class Person {
        public String name;
        public Long age;
        public Person bestFriend;
        @GelLinkType(Person.class)
        public Collection<Person> friends;
    }

    private static final String INSERT_QUERY =
            "with a := (insert Person { name := 'Person A', age := 20 } unless conflict on .name)," +
                    "b := (insert Person { name := 'Person B', age := 21 } unless conflict on .name)," +
                    "c := (insert Person { name := 'Person C', age := 22, friends := b } unless conflict on .name)" +
                    "insert Person { name := 'Person D', age := 23, friends := { a, b, c }, best_friend := c } unless conflict on .name";

    @Override
    public CompletionStage<Void> run(GelClientPool clientPool) {
        return clientPool.execute(INSERT_QUERY)
                .thenCompose(v ->
                        clientPool.queryRequiredSingle(
                                Person.class,
                                "select Person { name, age, friends: { name, age, friends }, best_friend: { name, age, friends } } filter .name = 'Person D'"
                        ))
                .thenAccept(result -> logger.info("Person with links: {}", result));
    }
}