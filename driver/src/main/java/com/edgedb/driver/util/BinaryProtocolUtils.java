package com.edgedb.driver.util;

import com.edgedb.driver.binary.SerializableData;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BinaryProtocolUtils {
    public static final int DOUBLE_SIZE = 8;
    public static final int FLOAT_SIZE = 4;
    public static final int LONG_SIZE = 8;
    public static final int INT_SIZE = 4;
    public static final int SHORT_SIZE = 2;
    public static final int BYTE_SIZE = 1;
    public static final int CHAR_SIZE = 2;
    public static final int BOOL_SIZE = 1;
    public static final int UUID_SIZE = 16;

    public static int sizeOf(String s) {
        int size = 4;

        if(s != null) {
            size += s.getBytes(StandardCharsets.UTF_8).length; // TODO: better way to do this?
        }

        return size;
    }

    public static int sizeOf(byte[] arr) {
        int size = 4;

        if(arr != null) {
            size += arr.length;
        }

        return size;
    }

    public static int sizeOf(ByteBuffer arr) {
        int size = 4;

        if(arr != null) {
            size += arr.position();
        }

        return size;
    }

    public static <T extends SerializableData> int sizeOf(T[] arr) {
        return Arrays.stream(arr).mapToInt(T::getSize).sum() + 4;
    }
}
