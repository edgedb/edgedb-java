package com.gel.driver;

/**
 * An enum specifying the client type to use within a client pool.
 */
public enum ClientType {
    /**
     * A TCP client using the Gel Binary protocol.
     */
    TCP,

    /**
     * An HTTP client using the Gel Binary protocol.
     */
    HTTP
}
