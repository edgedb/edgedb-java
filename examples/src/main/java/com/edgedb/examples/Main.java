package com.edgedb.examples;

import com.edgedb.driver.EdgeDBClient;
import com.edgedb.driver.EdgeDBClientConfig;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.namingstrategies.NamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, EdgeDBException, ExecutionException, InterruptedException {
        var client = new EdgeDBClient(new EdgeDBClientConfig() {{
            setNamingStrategy(NamingStrategy.snakeCase());
        }});

        // use the example module
        var exampleClient = client.withModule("examples");
        var examples = new ArrayList<Supplier<AbstractTypes>>() {
            {
                add(AbstractTypes::new);
            }
        };

        try {
            CompletableFuture
                    .allOf(examples.stream().map(v -> v.get().run(exampleClient).toCompletableFuture()).toArray(java.util.concurrent.CompletableFuture[]::new))
                    .get();
        }
        catch (Exception x) {
            logger.error("Failed to run examples", x);
        }

    }
}