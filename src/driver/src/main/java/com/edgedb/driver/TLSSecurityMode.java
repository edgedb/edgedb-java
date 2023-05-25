package com.edgedb.driver;

/**
 * Represents the TLS security mode the client will follow.
 */
public enum TLSSecurityMode {
    /**
     * Certificates and hostnames will be verified. This is the default behavior.
     */
    STRICT,

    /**
     * Verify certificates but not hostnames.
     */
    NO_HOSTNAME_VERIFICATION,

    /**
     * Client libraries will trust self-signed TLS certificates. useful for self-signed or custom certificates.
     */
    INSECURE,

    /**
     * The default value, equivalent to {@linkplain TLSSecurityMode#STRICT}
     */
    DEFAULT,
}
