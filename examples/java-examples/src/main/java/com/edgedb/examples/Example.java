package com.edgedb.examples;

import com.edgedb.driver.EdgeDBClient;

import java.util.concurrent.CompletionStage;

public interface Example {
    CompletionStage<Void> run(EdgeDBClient client) throws Exception;
}