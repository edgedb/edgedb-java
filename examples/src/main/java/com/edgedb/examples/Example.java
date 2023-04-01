package com.edgedb.examples;

import com.edgedb.driver.EdgeDBQueryable;

import java.util.concurrent.CompletionStage;

public interface Example {
    CompletionStage<Void> run(EdgeDBQueryable client);
}
