package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.binary.duplexers.Duplexer;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

public abstract class EdgeDBBinaryClient extends BaseEdgeDBClient {
    private static final int PROTOCOL_MAJOR_VERSION = 1;
    private static final int PROTOCOL_MINOR_VERSION = 0;
    private boolean isIdle;
    protected Duplexer duplexer;

    public EdgeDBBinaryClient(EdgeDBConnection connection) {
        super(connection);
    }

    public boolean getIsIdle() {
        return isIdle;
    }

    protected void setIsIdle(boolean value) {
        isIdle = value;
    }

    protected void setDuplexer(Duplexer duplexer) {
        this.duplexer = duplexer;
    }

    @Override
    public CompletionStage<Void> executeAsync(String query, Hashtable<String, Object> args) {
        return null;
    }

    @Override
    public <T> CompletionStage<List<T>> queryAsync(String query, Hashtable<String, Object> args) {
        return null;
    }

    @Override
    public <T> CompletionStage<T> querySingleAsync(String query, Hashtable<String, Object> args) {
        return null;
    }

    @Override
    public <T> CompletionStage<T> queryRequiredSingleAsync(String query, Hashtable<String, Object> args) {
        return null;
    }

    public abstract CompletionStage<Void> openConnection() throws GeneralSecurityException, IOException, TimeoutException;
    public abstract CompletionStage<Void> closeConnection();
}
