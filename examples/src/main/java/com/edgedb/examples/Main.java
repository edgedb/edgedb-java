package com.edgedb.examples;

import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.TLSSecurityMode;
import com.edgedb.driver.clients.EdgeDBTCPClient;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws IOException, GeneralSecurityException, InterruptedException, ExecutionException {

        var conn = new EdgeDBConnection();
        conn.setUsername("java");
        conn.setPassword("word");

        conn.setTLSSecurity(TLSSecurityMode.INSECURE);

        var client = new EdgeDBTCPClient(conn);

        client.connectAsync().whenComplete((v,e) -> {
            e.printStackTrace();
        });



        Thread.sleep(500000);
    }
}