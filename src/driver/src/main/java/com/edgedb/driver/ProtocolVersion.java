package com.edgedb.driver;


import org.joou.UShort;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents a <a href="https://www.edgedb.com/docs/reference/protocol/index#binary-protocol">EdgeDB Binary Protocol</a>
 * version, used to negotiate and specify which protocol version to use.
 */
public final class ProtocolVersion {
    /**
     * Gets the default {@linkplain ProtocolVersion} used by this version of the binding.
     */
    public static final ProtocolVersion BINARY_PROTOCOL_DEFAULT_VERSION = of(2, 0);

    private static final ConcurrentMap<Integer, ProtocolVersion> VERSIONS = new ConcurrentHashMap<>() {{
        put(keyOf(1, 0), new ProtocolVersion(1, 0));
        put(keyOf(2, 0), new ProtocolVersion(2, 0));
    }};

    /**
     * The major part of the version
     */
    public final short major;

    /**
     * The minor part of the version.
     */
    public final short minor;

    private ProtocolVersion(int major, int minor) {
        this.major = (short)(major & 0xFFFF);
        this.minor = (short)(minor & 0xFFFF);
    }

    private ProtocolVersion(int version) {
        this.major = (short)((version >> 16) & 0xFFFF);
        this.minor = (short)(version & 0xFFFF);
    }

    /**
     * Gets a protocol version representing the specified components.
     * @param major The major component of the version.
     * @param minor The minor component of the version.
     * @return a protocol version matching the provided components.
     */
    public static ProtocolVersion of(int major, int minor) {
        return VERSIONS != null
                ? VERSIONS.computeIfAbsent(keyOf(major, minor), ProtocolVersion::new)
                : new ProtocolVersion(major, minor);
    }

    /**
     * Gets a protocol version representing the specified components.
     * @param major The major component of the version.
     * @param minor The minor component of the version.
     * @return a protocol version matching the provided components.
     */
    public static ProtocolVersion of(UShort major, UShort minor) {
        return of(major.intValue(), minor.intValue());
    }

    private static int keyOf(int major, int minor) {
        return ((major & 0xFFFF) << 16) + (minor & 0xFFFF);
    }

    /**
     * Determines if the provided components equal this {@linkplain ProtocolVersion}
     * @param major The major component
     * @param minor The minor component
     * @return {@code true} if the components equal this {@linkplain ProtocolVersion}; otherwise {@code false}
     */
    public boolean equals(UShort major, UShort minor) {
        return equals(major.intValue(), minor.intValue());
    }

    /**
     * Determines if the provided components equal this {@linkplain ProtocolVersion}
     * @param major The major component
     * @param minor The minor component
     * @return {@code true} if the components equal this {@linkplain ProtocolVersion}; otherwise {@code false}
     */
    public boolean equals(int major, int minor) {
        return this.major == major && this.minor == minor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%d.%d", major, minor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ProtocolVersion)) {
            return super.equals(obj);
        }

        var other = (ProtocolVersion)obj;

        return this.major == other.major && this.minor == other.minor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(major, minor);
    }
}
