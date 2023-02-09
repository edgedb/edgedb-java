package com.edgedb;

import com.edgedb.driver.EdgeDBConnection;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
public class Main {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, KeyManagementException, CertificateException, KeyStoreException {
        var conn = EdgeDBConnection.resolveEdgeDBTOML();

        var cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(conn.getTLSCertificateAuthority().getBytes(StandardCharsets.US_ASCII)));

        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        keyStore.load(null, null);
        keyStore.setCertificateEntry("server", cert);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);


        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
        SSLSocketFactory factory = (SSLSocketFactory) sc.getSocketFactory();

        SSLSocket sslSocket = (SSLSocket) factory.createSocket("localhost", 10708);
        SSLParameters sslp = sslSocket.getSSLParameters();

        sslp.setApplicationProtocols(new String[] { "edgedb-binary" });

        sslSocket.setSSLParameters(sslp);

        sslSocket.startHandshake();

        String ap = sslSocket.getApplicationProtocol();
        var inputStream = sslSocket.getInputStream();
        var outputStream = sslSocket.getOutputStream();

        outputStream.write(new byte[] {
                0x56,
                0x00, 0x00, 0x00, 0x08,
                0x00, 0x01,
                0x00, 0x00,
                0x00, 0x00,
                0x00, 0x00
        });


        var buf = inputStream.readNBytes(5);
        //socket.
        System.out.println(buf);
    }
}