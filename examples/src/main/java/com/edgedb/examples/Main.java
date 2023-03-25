package com.edgedb.examples;

import com.edgedb.driver.EdgeDBClientConfig;
import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.clients.EdgeDBTCPClient;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws IOException, GeneralSecurityException, InterruptedException, ExecutionException {

        var client = new EdgeDBTCPClient(EdgeDBConnection.resolveEdgeDBTOML(), EdgeDBClientConfig.getDefault());

        client.connectAsync()
                .thenCompose((v) -> client.queryAsync("select 'Hello, Java!'", String.class))
                .whenComplete((v, e) -> {
                    var str = v.get(0);
                    System.out.println(v);
                });

        Thread.sleep(500000);
    }
}