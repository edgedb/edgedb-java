package com.gel.examples;

import com.gel.driver.GelClientPool;

import java.util.concurrent.CompletionStage;

public interface Example {
    CompletionStage<Void> run(GelClientPool clientPool);
}