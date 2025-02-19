package com.gel.driver.datatypes;

/**
 * A class representing the {@code cfg::memory} type.
 */
public final class Memory {
    private final long bytes;

    /**
     * Constructs a new {@linkplain Memory} type.
     * @param bytes The number of bytes describing this {@linkplain Memory} class.
     */
    public Memory(long bytes) {
        this.bytes = bytes;
    }

    /**
     * Gets the number of bytes within this {@linkplain Memory}.
     * @return The total number of bytes.
     */
    public long getBytes() {
        return bytes;
    }
}
