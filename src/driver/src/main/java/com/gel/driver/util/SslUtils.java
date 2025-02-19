package com.gel.driver.util;

import com.gel.driver.GelConnection;
import com.gel.driver.TLSSecurityMode;
import io.netty.handler.ssl.SslContextBuilder;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class SslUtils {
    public static final X509TrustManager INSECURE_TRUST_MANAGER = new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
        public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
        }
        public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
        }
    };

    public static void initContextWithConnectionDetails(
            @NotNull SSLContext context, @NotNull GelConnection connection)
    throws KeyManagementException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        if(connection.getTLSSecurity() == TLSSecurityMode.INSECURE) {
            context.init(null, new TrustManager[] {INSECURE_TRUST_MANAGER}, null);
            return;
        }

        context.init(null, getTrustManagerFactory(connection).getTrustManagers(), null);
    }

    public static void applyTrustManager(@NotNull GelConnection connection, @NotNull SslContextBuilder builder) throws GeneralSecurityException, IOException {
        if(connection.getTLSSecurity() == TLSSecurityMode.INSECURE) {
            builder.trustManager(INSECURE_TRUST_MANAGER);
        }
        else {
            builder.trustManager(getTrustManagerFactory(connection));
        }
    }

    public static @NotNull TrustManagerFactory getTrustManagerFactory(@NotNull GelConnection connection) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
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
