package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBClientConfig;
import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.TransactionState;
import com.edgedb.driver.binary.duplexers.Duplexer;
import com.edgedb.driver.binary.duplexers.HttpDuplexer;
import com.edgedb.driver.exceptions.ConnectionFailedException;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.exceptions.ScramException;
import com.edgedb.driver.util.Scram;
import com.edgedb.driver.util.SslUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public final class EdgeDBHttpClient extends EdgeDBBinaryClient {
    private static final Logger logger = LoggerFactory.getLogger(EdgeDBHttpClient.class);
    private static final String HTTP_TOKEN_AUTH_METHOD = "SCRAM-SHA-256";
    private final HttpDuplexer duplexer;
    public final HttpClient httpClient;

    private @Nullable String authToken;
    private @Nullable URI baseUri;
    private @Nullable URI authUri;
    private @Nullable URI execUri;

    public EdgeDBHttpClient(EdgeDBConnection connection, EdgeDBClientConfig config, AutoCloseable poolHandle) throws EdgeDBException {
        super(connection, config, poolHandle);
        this.duplexer = new HttpDuplexer(this);
        SSLContext context;
        try {
            context = SSLContext.getInstance("TLS");
            SslUtils.initContextWithConnectionDetails(context, getConnectionArguments());
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException |
                 KeyManagementException e) {
            throw new EdgeDBException("Failed to initialize SSL context", e);
        }

        this.httpClient = HttpClient.newBuilder()
                .sslContext(context)
                .build();
    }

    public String getToken() {
        return this.authToken;
    }

    public void clearToken() {
        this.authToken = null;
    }

    private CompletionStage<String> authenticate() {
        return CompletableFuture
                .supplyAsync(() -> {
                    logger.debug("Creating SCRAM and initial auth message...");
                    var scram = new Scram();

                    var first = scram.buildInitialMessage(getConnectionArguments().getUsername());

                    var request = HttpRequest.newBuilder()
                            .uri(getAuthUri())
                            .version(HttpClient.Version.HTTP_2)
                            .header(
                                    "Authorization",
                                    HTTP_TOKEN_AUTH_METHOD
                                            + " data="
                                            + Base64.getEncoder().encodeToString(first.getBytes(StandardCharsets.UTF_8)))
                            .GET()
                            .timeout(Duration.of(
                                    getConfig().getMessageTimeoutValue(),
                                    getConfig().getMessageTimeoutUnit().toChronoUnit()))
                            .build();

                    return Map.entry(scram, request);
                })
                .thenCompose(entry -> {
                    logger.debug("Executing initial auth request");

                    return httpClient.sendAsync(entry.getValue(), HttpResponse.BodyHandlers.ofByteArray())
                            .thenApply(response -> Map.entry(entry.getKey(), response));
                })
                .thenCompose(entry -> {
                    var authenticate = entry.getValue().headers().firstValue("www-authenticate");

                    logger.debug(
                            "Verifying response authenticate, is match?: {}",
                            authenticate.isPresent() && authenticate.get().startsWith(HTTP_TOKEN_AUTH_METHOD)
                    );

                    if(authenticate.isEmpty()) {
                        return CompletableFuture.failedFuture(
                                new ProtocolException("The only supported auth method is " + HTTP_TOKEN_AUTH_METHOD)
                        );
                    }

                    var authenticateData = authenticate.get().substring(HTTP_TOKEN_AUTH_METHOD.length() + 1);

                    var keys = parseKeys(authenticateData);

                    Scram.SASLFinalMessage finalMsg;

                    try {
                        logger.debug("Building final message...");
                        finalMsg = entry.getKey().buildFinalMessage(
                                new String(Base64.getDecoder().decode(keys.get("data")), StandardCharsets.UTF_8),
                                getConnectionArguments().getPassword()
                        );
                    } catch (ScramException e) {
                        logger.debug("Failed to build final message", e);
                        return CompletableFuture.failedFuture(e);
                    }

                    String payload = "sid=" +
                            keys.get("sid") +
                            " data=" +
                            Base64.getEncoder().encodeToString(finalMsg.message.getBytes(StandardCharsets.UTF_8));

                    var request = HttpRequest.newBuilder()
                            .uri(getAuthUri())
                            .header(
                                    "Authorization",
                                    HTTP_TOKEN_AUTH_METHOD + " " + payload
                            )
                            .GET()
                            .build();

                    logger.debug("Sending final auth message...");

                    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                })
                .thenCompose(EdgeDBHttpClient::ensureSuccess)
                .thenApply(HttpResponse::body);
    }

    public static <T> CompletionStage<HttpResponse<T>> ensureSuccess(HttpResponse<T> response) {
        logger.debug("Verifying success for code {}", response.statusCode());
        if(response.statusCode() / 100 != 2) {
            return CompletableFuture.failedFuture(
                    new ConnectionFailedException(
                            "Could not authenticate: " + response.statusCode()
                    )
            );
        }

        return CompletableFuture.completedFuture(response);
    }

    private static <T, U> CompletionStage<Map.Entry<T, HttpResponse<U>>> ensureSuccess(
            Map.Entry<T, HttpResponse<U>> entry
    ) {
        return ensureSuccess(entry.getValue())
                .thenApply(v -> entry);
    }

    private Map<String, String> parseKeys(String s) {
        return Arrays.stream(s.split(","))
                .map(v -> v.split("="))
                .collect(Collectors.toMap(
                        v -> v[0].trim(),
                        v -> v[1]
                ));
    }

    public synchronized URI getAuthUri() {
        if(authUri != null) {
            return authUri;
        }

        return authUri = getBaseUri().resolve("/auth/token");
    }

    public synchronized URI getBaseUri() {
        if(baseUri != null) {
            return baseUri;
        }

        return baseUri = URI.create(
                "https://" + getConnectionArguments().getHostname() + ":" + getConnectionArguments().getPort()
        );
    }

    public synchronized URI getExecUri() {
        if(execUri != null) {
            return execUri;
        }

        return execUri = getBaseUri().resolve("/db/" + getConnectionArguments().getDatabase());
    }

    @Override
    public Duplexer getDuplexer() {
        return this.duplexer;
    }

    @Override
    public void setTransactionState(TransactionState state) {
        // invalid for this client
    }

    @Override
    protected CompletionStage<Void> openConnection() {
        return CompletableFuture.completedFuture(null); // not valid for this client.
    }

    @Override
    public CompletionStage<Void> connect() {
        if(authToken == null) {
            return authenticate()
                    .thenAccept(token -> this.authToken = token);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletionStage<Void> closeConnection() {
        return CompletableFuture.completedFuture(null);
    }
}
