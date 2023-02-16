package com.edgedb.examples;

import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.clients.EdgeDBTCPClient;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;

public class Main {

    public static void main(String[] args) throws IOException, GeneralSecurityException, InterruptedException, ExecutionException {
        //var pool = Executors.newCachedThreadPool();
        var conn = EdgeDBConnection.resolveEdgeDBTOML();

        var client = new EdgeDBTCPClient(conn);

        var cls = Integer.class;

        Long v = 0x00_00_FF_FF_CDL;
        Integer target = 0x00FFFFCD;

        //var c2 = Math.toIntExact(v);
        int c = v.intValue();

        var a = cls.cast(v);

        System.out.println(a);


//        var cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(conn.getTLSCertificateAuthority().getBytes(StandardCharsets.US_ASCII)));
//
//        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//
//        keyStore.load(null, null);
//        keyStore.setCertificateEntry("server", cert);
//
//        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//        trustManagerFactory.init(keyStore);
//
//
//        SSLContext sc = SSLContext.getInstance("SSL");
//
//        SSLAsynchronousChannelGroup group = new SSLAsynchronousChannelGroup();
//        SSLAsynchronousSocketChannel channel = SSLAsynchronousSocketChannel.open(group, true);
//
//        channel.initSSLContext(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
//        channel.connect(new InetSocketAddress("localhost", 10708), null, new CompletionHandler<Void, Object>() {
//            @Override
//            public void completed(Void result, Object attachment) {
//                try {
//                    channel.write(
//                            ByteBuffer.wrap(HexUtils.hexStringToByteArray("5600000034000100000002000000047573657200000006656467656462000000086461746162617365000000066564676564620000")),
//                            10, TimeUnit.SECONDS,
//                            null,
//                            new CompletionHandler<Integer, Object>() {
//                                @Override
//                                public void completed(Integer result, Object attachment) {
//                                    var b = ByteBuffer.allocate(5);
//
//                                    channel.read(b, 10, TimeUnit.SECONDS, b, new CompletionHandler<Integer, ByteBuffer>() {
//                                        @Override
//                                        public void completed(Integer result, ByteBuffer attachment) {
//                                            System.out.println("A");
//                                        }
//
//                                        @Override
//                                        public void failed(Throwable exc, ByteBuffer attachment) {
//
//                                        }
//                                    });
//                                }
//
//                                @Override
//                                public void failed(Throwable exc, Object attachment) {
//
//                                }
//                            }
//                    );
//                } catch (SSLException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//            @Override
//            public void failed(Throwable exc, Object attachment) {
//
//            }
//        });
//
//        Thread.sleep(50000);
//
//
//        sc.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
//        SSLSocketFactory factory = (SSLSocketFactory) sc.getSocketFactory();
//
//        SSLSocket sslSocket = (SSLSocket) factory.createSocket("localhost", 10708);
//        SSLParameters sslp = sslSocket.getSSLParameters();
//
//        sslp.setApplicationProtocols(new String[] { "edgedb-binary" });
//
//        sslSocket.setSSLParameters(sslp);
//
//        sslSocket.startHandshake();
//
//        String ap = sslSocket.getApplicationProtocol();
//        var inputStream = sslSocket.getInputStream();
//        var outputStream = sslSocket.getOutputStream();
//
//        outputStream.write(HexUtils.hexStringToByteArray("5600000034000100000002000000047573657200000006656467656462000000086461746162617365000000066564676564620000"));
//
//
//        var buf = inputStream.readNBytes(5);
//        //socket.
//        System.out.println(buf);
    }
}