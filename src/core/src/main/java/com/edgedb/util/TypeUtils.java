package com.edgedb.util;

import java.lang.reflect.Array;

public class TypeUtils {

    public static Object getDefaultValue(Class<?> cls) {
        return Array.get(Array.newInstance(cls, 1), 0);
    }
}
