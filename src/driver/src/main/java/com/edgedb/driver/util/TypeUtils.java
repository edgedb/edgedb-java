package com.edgedb.driver.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joou.UByte;
import org.joou.UInteger;
import org.joou.ULong;
import org.joou.UShort;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.joou.Unsigned.*;
import static org.joou.Unsigned.ulong;

public class TypeUtils {
    public static final Map<Class<?>, Class<?>> PRIMITIVE_REFERENCE_MAP = new HashMap<>(){{
        put(Byte.class, Byte.TYPE);
        put(Short.class, Short.TYPE);
        put(Integer.class, Integer.TYPE);
        put(Long.class, Long.TYPE);
        put(Float.class, Float.TYPE);
        put(Double.class, Double.TYPE);
        put(Character.class, Character.TYPE);
        put(Boolean.class, Boolean.TYPE);
        put(Byte.TYPE, Byte.class);
        put(Short.TYPE, Short.class);
        put(Integer.TYPE, Integer.class);
        put(Long.TYPE, Long.class);
        put(Float.TYPE, Float.class);
        put(Double.TYPE, Double.class);
        put(Character.TYPE, Character.class);
        put(Boolean.TYPE, Boolean.class);
    }};
    private static final @NotNull Map<Class<?>, Function<Number, ?>> PRIMITIVE_NUMBER_CAST_MAP;

    static {
        PRIMITIVE_NUMBER_CAST_MAP = new HashMap<>() {
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

    @SuppressWarnings("unchecked")
    public static <T extends Number, U extends Number> U castToPrimitiveNumber(T value, Class<U> target) {
        return (U) PRIMITIVE_NUMBER_CAST_MAP.get(target).apply(value);
    }

    public static Object getDefaultValue(Class<?> cls) {
        return Array.get(Array.newInstance(cls, 1), 0);
    }

    public static @Nullable Class<?> tryPullWrappingType(@NotNull Class<?> cls) {
        if(cls.getComponentType() != null) {
            return cls.getComponentType();
        }

        return null;
    }
}
