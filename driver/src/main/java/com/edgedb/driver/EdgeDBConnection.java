package com.edgedb.driver;

import com.edgedb.driver.exceptions.ConfigurationException;
import com.edgedb.driver.util.ConfigUtils;
import com.edgedb.driver.util.EnumsUtil;
import com.edgedb.driver.util.QueryParamUtils;
import com.edgedb.driver.util.StringsUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.netty.handler.ssl.SslContextBuilder;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

public class EdgeDBConnection {
    private static final String EDGEDB_INSTANCE_ENV_NAME = "EDGEDB_INSTANCE";
    private static final String EDGEDB_DSN_ENV_NAME = "EDGEDB_DSN";
    private static final String EDGEDB_CREDENTIALS_FILE_ENV_NAME = "EDGEDB_CREDENTIALS_FILE";
    private static final String EDGEDB_USER_ENV_NAME = "EDGEDB_USER";
    private static final String EDGEDB_PASSWORD_ENV_NAME = "EDGEDB_PASSWORD";
    private static final String EDGEDB_DATABASE_ENV_NAME = "EDGEDB_DATABASE";
    private static final String EDGEDB_HOST_ENV_NAME = "EDGEDB_HOST";
    private static final String EDGEDB_PORT_ENV_NAME = "EDGEDB_PORT";

    private static final Pattern DSN_FORMATTER = Pattern.compile("^([a-z]+)://");
    private static final Pattern DSN_QUERY_PARAMETERS = Pattern.compile("((?:.(?!\\?))+$)");
    private static final Pattern DSN_FILE_ARG = Pattern.compile("(.*?)_file");
    private static final Pattern DSN_ENV_ARG = Pattern.compile("(.*?)_env");

    @JsonProperty("user")
    private String user;

    @JsonProperty("password")
    private String password;

    @JsonProperty("database")
    private String database;

    @JsonIgnore
    private String hostname;

    @JsonProperty("port")
    private Integer port;

    @JsonProperty("tls_ca")
    private String tlsca;

    @JsonProperty("tls_security")
    private TLSSecurityMode tlsSecurity;

    public String getUsername() {
        return user == null ? "edgedb" : user;
    }
    public void setUsername(String value) {
        user = value;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String value) {
        password = value;
    }

    public String getHostname() {
        return hostname == null ? "127.0.0.1" : hostname;
    }
    public void setHostname(String value) throws ConfigurationException {
        if (value.contains("/")) {
            throw new ConfigurationException("Cannot use UNIX socket for 'Hostname'");
        }

        if (value.contains(",")) {
            throw new ConfigurationException("DSN cannot contain more than one host");
        }

        hostname = value;
    }

    public int getPort() {
        return port == null ? 5656 : port;
    }
    public void setPort(int value) {
        port = value;
    }

    public String getDatabase() {
        return database == null ? "edgedb" : database;
    }
    public void setDatabase(String value) {
        database = value;
    }

    public String getTLSCertificateAuthority() {
        return tlsca;
    }
    public void setTLSCertificateAuthority(String value) {
        tlsca = value;
    }

    public TLSSecurityMode getTLSSecurity() {
        return tlsSecurity;
    }

    public void setTLSSecurity(TLSSecurityMode value) {
        tlsSecurity = value;
    }

    public static EdgeDBConnection fromDSN(@NotNull String dsn) throws ConfigurationException, IOException {
        if (!dsn.startsWith("edgedb://")) {
            throw new ConfigurationException("Invalid DSN protocol. the DSN schema 'edgedb://...' is expected");
        }

        String database = null, username = null, port = null, host = null, password = null;

        Map<String, String> args = Collections.emptyMap();

        var formattedDSN = DSN_FORMATTER.matcher(dsn).replaceAll("");

        var queryParams = DSN_QUERY_PARAMETERS.matcher((dsn));

        if (queryParams.matches()){
            args = QueryParamUtils.splitQuery(queryParams.group(1).substring(1));

            // remove args from formatted dsn
            formattedDSN = formattedDSN.replace(queryParams.group(1), "");
        }

        var partA = formattedDSN.split("/");

        if (partA.length == 2) {
            database = partA[1];
            formattedDSN = partA[0];
        }

        var partB = formattedDSN.split("@");

        if (partB.length == 2 && !partB[1].equals("")) {
            if (partB[1].contains(","))
                throw new ConfigurationException("DSN cannot contain more than one host");

            var right = partB[1].split(":");

            if(right.length == 2) {
                host = right[0];
                port = right[1];
            }
            else {
                host = right[0];
            }

            var left = partB[0].split(":");

            if(left.length == 2) {
                username = left[0];
                password = left[1];
            }
            else {
                username = left[0];
            }
        }
        else {
            var sub = partB[0].split(":");

            if(sub.length == 2) {
                host = sub[0];
                port = sub[1];
            }
            else {
                host = sub[0];
            }
        }

        var connection = new EdgeDBConnection();

        if(database != null)
            connection.setDatabase(database);

        if(host != null)
            connection.setHostname(host);

        if(username != null)
            connection.setUsername(username);

        if(password != null)
            connection.setPassword(password);

        if(port != null) {
            try{
                connection.setPort(Integer.parseInt(port));
            }
            catch (NumberFormatException e) {
                throw new ConfigurationException("port was not in the correct format", e);
            }
        }

        for (var entry : args.entrySet()) {
            var fileMatch = DSN_FILE_ARG.matcher(entry.getKey());
            var envMatch = DSN_ENV_ARG.matcher(entry.getKey());

            String value;
            String key;

            if(fileMatch.matches()){
                key = fileMatch.group(1);

                var file = new File(entry.getValue());

                if(!file.exists()) {
                    throw new FileNotFoundException(String.format("The specified argument \"%s\"'s file was not found", key));
                }

                if(!file.isFile()) {
                    throw new IllegalArgumentException(String.format("The specified argument \"%s\" is not a file", key));
                }

                if(!file.canRead()) {
                    throw new IllegalArgumentException(String.format("The specified argument \"%s\"'s file cannot be read: missing permissions", key));
                }

                value = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }
            else if (envMatch.matches()) {
                key = entry.getKey();
                value = System.getenv(entry.getValue());

                if (value == null) {
                    throw new ConfigurationException(String.format("Enviroment variable \"%s\" couldn't be found", entry.getValue()));
                }
            }
            else  {
                key = entry.getKey();
                value = entry.getValue();
            }

            setArgument(connection, key, value);
        }

        return connection;
    }

    public static EdgeDBConnection fromProjectFile(Path path) throws IOException {
        return fromProjectFile(path.toFile());
    }
    public static EdgeDBConnection fromProjectFile(String path) throws IOException {
        return fromProjectFile(new File(path));
    }

    public static EdgeDBConnection fromProjectFile(File file) throws IOException {
        if(!file.exists()) {
            throw new FileNotFoundException("Couldn't find the specified project file");
        }

        file = file.getAbsoluteFile();

        var dirName = file.getParent();

        var projectDir = Paths.get(ConfigUtils.getInstanceProjectDirectory(dirName));

        if(!Files.isDirectory(projectDir)) {
            throw new FileNotFoundException(String.format("Couldn't find project directory for %s: %s", file, projectDir));
        }

        var instanceName = Files.readString(projectDir.resolve("instance-name"), StandardCharsets.UTF_8);

        return fromInstanceName(instanceName);
    }

    public static EdgeDBConnection fromInstanceName(String instanceName) throws IOException {
        var configPath = Paths.get(ConfigUtils.getCredentialsDir(), instanceName + ".json");

        if(!Files.exists(configPath))
            throw new FileNotFoundException("Config file couldn't be found at " + configPath);

        return fromJSON(Files.readString(configPath, StandardCharsets.UTF_8));
    }

    public static EdgeDBConnection resolveEdgeDBTOML() throws IOException {
        var dir = Paths.get(System.getProperty("user.dir"));

        while(true) {
            var target = dir.resolve("edgedb.toml");

            if (Files.exists(target)) {
                return fromProjectFile(target);
            }

            var parent = dir.getParent();

            if(parent == null || !Files.exists(parent)) {
                throw new FileNotFoundException("Couldn't resolve edgedb.toml file");
            }

            dir = parent;
        }
    }

    public static EdgeDBConnection parse(String connection) {
        // TODO: implement this method and add a builder method for parse
        return new EdgeDBConnection();
    }

    public SSLContext getSSLContext() throws GeneralSecurityException, IOException {
        return getSSLContext("SSL");
    }
    public SSLContext getSSLContext(String instanceName) throws GeneralSecurityException, IOException {
        SSLContext sc = SSLContext.getInstance(instanceName);
        TrustManager[] trustManagers;

        if(this.tlsSecurity == TLSSecurityMode.INSECURE) {
            trustManagers = new TrustManager[] { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }};
        }
        else {
            trustManagers = getTrustManagerFactory().getTrustManagers();
        }

        sc.init(null, trustManagers , new SecureRandom());
        return sc;
    }

    public void applyTrustManager(SslContextBuilder builder) throws GeneralSecurityException, IOException {
        if(this.tlsSecurity == TLSSecurityMode.INSECURE) {
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
            builder.trustManager(getTrustManagerFactory());
        }
    }

    private TrustManagerFactory getTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        var authority = getTLSCertificateAuthority();
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        if (StringsUtil.isNullOrEmpty(authority)) {
            // use default trust store
            trustManagerFactory.init((KeyStore)null);
            //throw new ConfigurationException("TLSCertificateAuthority cannot be null when TLSSecurity is STRICT");
        }
        else {
            var cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(getTLSCertificateAuthority().getBytes(StandardCharsets.US_ASCII)));

            var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            keyStore.load(null, null);
            keyStore.setCertificateEntry("server", cert);

            trustManagerFactory.init(keyStore);
        }

        return trustManagerFactory;
    }

    private static EdgeDBConnection fromJSON(String json) throws JsonProcessingException {
        var mapper = JsonMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();

        return mapper.readValue(json, EdgeDBConnection.class);
    }

    private static void setArgument(EdgeDBConnection connection, String name, String value) throws ConfigurationException, IllegalArgumentException, IOException {
        if (StringsUtil.isNullOrEmpty(value))
            return;

        switch (name) {
            case "port":
                if (connection.port != null) {
                    throw new IllegalArgumentException("Port ambiguity mismatch");
                }

                try {
                    connection.setPort(Integer.parseInt(value));
                }
                catch (NumberFormatException e) {
                    throw new ConfigurationException("port was not in the correct format", e);
                }
                break;
            case "host":
                if (connection.hostname != null) {
                    throw new IllegalArgumentException("Host ambiguity mismatch");
                }

                connection.setHostname(value);
                break;
            case "user":
                if (connection.user != null) {
                    throw new IllegalArgumentException("User ambiguity mismatch");
                }

                connection.setUsername(value);
                break;
            case "password":
                if (connection.password != null) {
                    throw new IllegalArgumentException("Password ambiguity mismatch");
                }

                connection.setPassword(value);
                break;
            case "tls_cert_file":
                var file = new File(value);

                if(!file.exists()) {
                    throw new FileNotFoundException("The specified tls_cert_file file was not found");
                }

                if(!file.isFile()) {
                    throw new IllegalArgumentException("The specified tls_cert_file is not a file");
                }

                if(!file.canRead()) {
                    throw new IllegalArgumentException("The specified tls_cert_file cannot be read: missing permissions");
                }

                connection.setTLSCertificateAuthority(Files.readString(file.toPath(), StandardCharsets.UTF_8));
                break;
            case "tls_security":
                var security = EnumsUtil.searchEnum(TLSSecurityMode.class, value);

                if(security == null) {
                    throw new IllegalArgumentException(String.format("\"%s\" must be a value of edgedb.driver.TLSSecurityMode", value));
                }

                connection.setTLSSecurity(security);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unexpected configuration option %s", name));
        }
    }
}
