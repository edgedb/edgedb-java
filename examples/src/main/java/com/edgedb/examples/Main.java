package com.edgedb.examples;

import com.edgedb.driver.EdgeDBClient;
import com.edgedb.EdgeDBClientConfig;
import com.edgedb.exceptions.EdgeDBException;
import com.edgedb.namingstrategies.NamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, EdgeDBException, ExecutionException, InterruptedException {
        var client = new EdgeDBClient(new EdgeDBClientConfig() {{
            setNamingStrategy(NamingStrategy.snakeCase());
            setUseFieldSetters(true);
        }});

        // use the example module
        var exampleClient = client.withModule("examples");
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
            logger.info("Running {}...", inst);
            try {
                inst.run(exampleClient).toCompletableFuture().get();
                logger.info("Example {} complete!", inst);
            }
            catch (Exception x) {
                logger.error("Failed to run example {}", inst, x);
            }
        }

        logger.info("Examples complete");
    }
}