package com.gel.driver.util;

import com.gel.driver.binary.SerializableData;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static int sizeOf(@Nullable String s) {
        int size = 4;

        if(s != null) {
            size += s.getBytes(StandardCharsets.UTF_8).length;
        }

        return size;
    }

    public static int sizeOf(@Nullable ByteBuf buffer) {
        int size = 4;

        if(buffer != null) {
            size += buffer.writerIndex();
        }

        return size;
    }

    public static <U extends Number> int sizeOf(@NotNull Class<U> primitive) {
        if(primitive == Long.TYPE || primitive == Long.class) {
            return LONG_SIZE;
        }
        else if(primitive == Integer.TYPE || primitive == Integer.class) {
            return INT_SIZE;
        }
        else if(primitive == Short.TYPE || primitive == Short.class) {
            return SHORT_SIZE;
        }
        else if(primitive == Byte.TYPE || primitive == Byte.class) {
            return BYTE_SIZE;
        }
        else if(primitive == Double.TYPE || primitive == Double.class) {
            return DOUBLE_SIZE;
        }
        else if(primitive == Float.TYPE || primitive == Float.class) {
            return FLOAT_SIZE;
        }

        throw new ArithmeticException("Unable to determine the size of " + primitive.getName());
    }

    public static <T extends SerializableData, U extends Number> int sizeOf(T @NotNull [] arr, @NotNull Class<U> primitive) {
        return Arrays.stream(arr).mapToInt(T::getSize).sum() + sizeOf(primitive);
    }
}
