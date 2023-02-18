package com.edgedb.examples;

import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.clients.EdgeDBTCPClient;
import com.edgedb.driver.ssl.AsyncSSLChannel;
import com.edgedb.driver.util.HexUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws IOException, GeneralSecurityException, InterruptedException, ExecutionException {

        var conn = EdgeDBConnection.resolveEdgeDBTOML();

        var channel = new AsyncSSLChannel(conn.getSSLContext().createSSLEngine(conn.getHostname(), conn.getPort()), null);
        //channel.setALPNProtocols("edgedb-binary");

        var writeBuff = ByteBuffer.wrap(HexUtils.hexStringToByteArray("5600000034000100000002000000047573657200000006656467656462000000086461746162617365000000066564676564620000"));

        channel
                .connect(conn.getHostname(), conn.getPort())
                .thenCompose((v) -> channel.write(writeBuff, 5000, TimeUnit.MILLISECONDS))
                .thenCompose((v) -> {
                    var buff = ByteBuffer.allocateDirect(5); // "HTTP " is what we want to read
                    return channel.readAsync(buff, 5000, TimeUnit.MILLISECONDS);
                })
                .whenCompleteAsync((v, e) -> {
                    System.out.println(e.getMessage());
                });

        Thread.sleep(50000);

        var client = new EdgeDBTCPClient(conn);

        client.connectAsync().whenComplete((v,e) -> {
            e.printStackTrace();
        });



        Thread.sleep(500000);

        // 5600000034000100000002000000047573657200000006656467656462000000086461746162617365000000066564676564620000
    }
}