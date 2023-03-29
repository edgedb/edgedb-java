package com.edgedb.examples;

import com.edgedb.driver.EdgeDBClientConfig;
import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.clients.EdgeDBTCPClient;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Main {
    public static final class Person {
        public String name;
        public Long age;
    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        var client = new EdgeDBTCPClient(EdgeDBConnection.resolveEdgeDBTOML(), EdgeDBClientConfig.getDefault());

        client.connect().toCompletableFuture().get();

        var result = client.query("select Person { name, age }", Person.class).toCompletableFuture().get();

        Thread.sleep(500000);
    }
}