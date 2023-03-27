package com.edgedb.driver.util;

import java.lang.reflect.Array;

public class TypeUtils {

    private static Object getDefaultValue(Class<?> cls) {
        return Array.get(Array.newInstance(cls, 1), 0);
    }
}
