package com.edgedb.driver.util;

import org.jetbrains.annotations.NotNull;

public final class FastInverseIndexer {
    private final int length;
    private final byte @NotNull [] buffer;
    private int trackedLength;

    public FastInverseIndexer(int count) {
        this.trackedLength = count;
        this.length = count;

        var c = count + 0b111;
        var t = (count >> 3) + ((count | c) >> 3);
        this.buffer = new byte[t];
    }

    public void set(int position) {
        trackedLength--;

        if(trackedLength < 0) {
            throw new IndexOutOfBoundsException("Too many items tracked");
        }

        var b = 1 << (position & 0b111);
        var i = position >> 3;
        buffer[i] |= (byte) b;
    }

    public int[] getInverseIndexes() {
        if(trackedLength == 0) {
            return new int[0];
        }

        var inverse = new int[trackedLength];

        int p = 0;

        for(int i = 0; i != buffer.length; i++) {
            var k = i << 3;
            var b = ~buffer[i];
            for(int j = 0; j != 8 && length > k + j; j++) {
                if((b & 1) == 1) {
                    inverse[p] = j + k;
                    p++;
                }
                b >>= 1;
            }
        }

        return inverse;
    }
}
