package com.edgedb.util;

public class EnumsUtil {
    public static <T extends Enum<?>> T searchEnum(Class<T> enumeration,
                                                   String search) {
        for (T each : enumeration.getEnumConstants()) {
            if (each.name().equalsIgnoreCase(search)) {
                return each;
            }
        }
        return null;
    }
}
