package com.edgedb.driver;

import org.jetbrains.annotations.NotNull;

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
    DEFAULT;

    public static TLSSecurityMode fromString(@NotNull String text) {
        return fromString(text, false);
    }

    public static TLSSecurityMode fromString(@NotNull String text, boolean emptyAsDefault) {

        // Capitalized text does not conform to other libraries,
        // but is supported for backwards compatibility.
        switch (text)
        {
            case "strict":
                return TLSSecurityMode.STRICT;
            case "no_host_verification":
                return TLSSecurityMode.NO_HOSTNAME_VERIFICATION;
            case "insecure":
            case "insecure_dev_mode":
                return TLSSecurityMode.INSECURE;
            case "default":
                return TLSSecurityMode.DEFAULT;
            case "":
                return emptyAsDefault ? TLSSecurityMode.DEFAULT : null;
        }
        return null;
    };
}
