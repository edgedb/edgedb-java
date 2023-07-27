package com.edgedb.driver.binary.protocol;


import org.joou.UShort;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ProtocolVersion {
    public static final ProtocolVersion BINARY_PROTOCOL_DEFAULT_VERSION = of(1, 0);

    private static final ConcurrentMap<Integer, ProtocolVersion> VERSIONS = new ConcurrentHashMap<>() {{
        put(keyOf(1, 0), new ProtocolVersion(1, 0));
        put(keyOf(2, 0), new ProtocolVersion(2, 0));
    }};

    public final short major;
    public final short minor;

    private ProtocolVersion(int major, int minor) {
        this.major = (short)(major & 0xFFFF);
        this.minor = (short)(minor & 0xFFFF);
    }

    private ProtocolVersion(int version) {
        this.major = (short)((version >> 16) & 0xFFFF);
        this.minor = (short)(version & 0xFFFF);
    }

    public static ProtocolVersion of(int major, int minor) {
        return VERSIONS != null
                ? VERSIONS.computeIfAbsent(keyOf(major, minor), ProtocolVersion::new)
                : new ProtocolVersion(major, minor);
    }

    public static ProtocolVersion of(UShort major, UShort minor) {
        return of(major.intValue(), minor.intValue());
    }

    private static int keyOf(int major, int minor) {
        return ((major & 0xFFFF) << 16) + (minor & 0xFFFF);
    }

    public boolean equals(UShort major, UShort minor) {
        return equals(major.intValue(), minor.intValue());
    }

    public boolean equals(int major, int minor) {
        return this.major == major && this.minor == minor;
    }

    @Override
    public String toString() {
        return String.format("%d.%d", major, minor);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ProtocolVersion)) {
            return super.equals(obj);
        }

        var other = (ProtocolVersion)obj;

        return this.major == other.major && this.minor == other.minor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor);
    }
}
