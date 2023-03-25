package com.edgedb.driver.util;

import com.edgedb.driver.datatypes.Range;

public class TypeUtils {
    public static Class<?> getWrappingType(Class<?> cls) {
        if(cls.isArray()) {
            return cls.getComponentType();
        }

        if(cls.equals(Range.EMPTY_RANGE.getClass())) {
            //return
        }

        return cls;
    }
}
