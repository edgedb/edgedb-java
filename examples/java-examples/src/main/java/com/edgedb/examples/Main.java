package com.edgedb.examples;

import com.edgedb.driver.EdgeDBClient;
import com.edgedb.driver.EdgeDBClientConfig;
import com.edgedb.driver.namingstrategies.NamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.function.Supplier;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        try (var client = new EdgeDBClient(EdgeDBClientConfig.builder()
                .withNamingStrategy(NamingStrategy.snakeCase())
                .useFieldSetters(true)
                .build()
        ).withModule("examples")) {
            runJavaExamples(client);

            logger.info("Examples complete");
        }

        // run a GC cycle to ensure that any remaining dormant client instances get collected and closed.
        System.gc();
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