package com.edgedb.driver.util;

import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.TLSSecurityMode;
import io.netty.handler.ssl.SslContextBuilder;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class SslUtils {
    public static void applyTrustManager(@NotNull EdgeDBConnection connection, @NotNull SslContextBuilder builder) throws GeneralSecurityException, IOException {
        if(connection.getTLSSecurity() == TLSSecurityMode.INSECURE) {
            builder.trustManager(new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            });
        }
        else {
            builder.trustManager(getTrustManagerFactory(connection));
        }
    }

    private static @NotNull TrustManagerFactory getTrustManagerFactory(@NotNull EdgeDBConnection connection) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        var authority = connection.getTLSCertificateAuthority();

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        if (StringsUtil.isNullOrEmpty(authority)) {
            // use default trust store
            trustManagerFactory.init((KeyStore)null);
            //throw new ConfigurationException("TLSCertificateAuthority cannot be null when TLSSecurity is STRICT");
        }
        else {
            var cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(authority.getBytes(StandardCharsets.US_ASCII)));

            var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            keyStore.load(null, null);
            keyStore.setCertificateEntry("server", cert);

            trustManagerFactory.init(keyStore);
        }

        return trustManagerFactory;
    }
}
