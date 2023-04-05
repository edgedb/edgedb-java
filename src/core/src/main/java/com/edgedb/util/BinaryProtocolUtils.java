package com.edgedb.util;

import com.edgedb.binary.SerializableData;
import io.netty.buffer.ByteBuf;
import org.joou.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.joou.Unsigned.*;

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

    private static final Map<Class<?>, Function<Number, ?>> numberCastMap;

    static {
        numberCastMap = new HashMap<>() {
            {
                put(Long.TYPE, Number::longValue);
                put(Integer.TYPE, Number::intValue);
                put(Short.TYPE, Number::shortValue);
                put(Byte.TYPE, Number::byteValue);
                put(Double.TYPE, Number::doubleValue);
                put(Float.TYPE, Number::floatValue);
                put(UByte.class, number -> ubyte(number.longValue()));
                put(UShort.class, number -> ushort(number.intValue()));
                put(UInteger.class, number -> uint(number.longValue()));
                put(ULong.class, number -> ulong(number.longValue()));
            }
        };
    }

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

    public static int sizeOf(ByteBuf buffer) {
        int size = 4;

        if(buffer != null) {
            size += buffer.writerIndex();
        }

        return size;
    }

    public static <U extends Number> int sizeOf(Class<U> primitive) {
        if(primitive == Long.TYPE) {
            return LONG_SIZE;
        }
        else if(primitive == Integer.TYPE) {
            return INT_SIZE;
        }
        else if(primitive == Short.TYPE) {
            return SHORT_SIZE;
        }
        else if(primitive == Byte.TYPE) {
            return BYTE_SIZE;
        }
        else if(primitive == Double.TYPE) {
            return DOUBLE_SIZE;
        }
        else if(primitive == Float.TYPE) {
            return FLOAT_SIZE;
        }

        // TODO: fail
        return 0;
    }
    @SuppressWarnings("unchecked")
    public static <T extends Number, U extends Number> U castNumber(T value, Class<U> target) {
        return (U) numberCastMap.get(target).apply(value);
    }

    public static <T extends SerializableData, U extends Number> int sizeOf(T[] arr, Class<U> primitive) {
        return Arrays.stream(arr).mapToInt(T::getSize).sum() + sizeOf(primitive);
    }
}
