package com.edgedb.driver.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

public class TypeUtils {

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
