package com.edgedb.examples;

import com.edgedb.driver.EdgeDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class GlobalsAndConfig implements Example {
    private static final Logger logger = LoggerFactory.getLogger(AbstractTypes.class);

    @Override
    public CompletionStage<Void> run(EdgeDBClient client) {
        var configuredClient = client
                .withConfig(config -> config
                        .withIdleTransactionTimeout(Duration.ZERO)
                        .applyAccessPolicies(true))
                .withGlobals(new HashMap<>(){{
                    put("current_user_id", UUID.randomUUID());
                }});

        return configuredClient.queryRequiredSingle(UUID.class, "select global current_user_id")
                .thenAccept(result -> logger.info("current_user_id global: {}", result));
    }
}