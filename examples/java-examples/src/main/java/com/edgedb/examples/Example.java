package com.edgedb.examples;

import com.edgedb.driver.GelClientPool;

import java.util.concurrent.CompletionStage;

public interface Example {
    CompletionStage<Void> run(GelClientPool clientPool);
}