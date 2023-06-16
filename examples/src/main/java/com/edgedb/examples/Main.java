package com.edgedb.examples;

import com.edgedb.driver.EdgeDBClient;
import com.edgedb.driver.EdgeDBClientConfig;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.namingstrategies.NamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, EdgeDBException, ExecutionException, InterruptedException {
        var argsList = Arrays.asList(args);

        var client = new EdgeDBClient(EdgeDBClientConfig.builder()
                .withNamingStrategy(NamingStrategy.snakeCase())
                .useFieldSetters(true)
                .build()
        );

        // use the example module
        var exampleClient = client.withModule("examples");

        if(argsList.contains("java")) {
            runJavaExamples(exampleClient);
        }

        if(argsList.contains("kotlin")) {
            com.edgedb.examples.KotlinMain.Companion.runExamples(exampleClient);
        }

        if(argsList.contains("scala")) {
            new ScalaMain().runExamples(exampleClient)
                    .toCompletableFuture().get();
        }

        logger.info("Examples complete");
    }

    private static void runJavaExamples(EdgeDBClient client) {
        var examples = new ArrayList<Supplier<Example>>() {
            {
                add(AbstractTypes::new);
                add(BasicQueryFunctions::new);
                add(CustomDeserializer::new);
                add(GlobalsAndConfig::new);
                add(JsonResults::new);
                add(LinkProperties::new);
                add(QueryResults::new);
                add(Transactions::new);
            }
        };

        for (var example : examples) {
            var inst = example.get();
            logger.info("Running Java example {}...", inst);
            try {
                inst.run(client).toCompletableFuture().get();
                logger.info("Java example {} complete!", inst);
            }
            catch (Exception x) {
                logger.error("Failed to run Java example {}", inst, x);
            }
        }
    }
}