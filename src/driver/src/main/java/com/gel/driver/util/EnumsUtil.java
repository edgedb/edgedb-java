package com.gel.driver.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnumsUtil {
    public static <T extends Enum<?>> @Nullable T searchEnum(@NotNull Class<T> enumeration,
                                                             String search) {
        for (T each : enumeration.getEnumConstants()) {
            if (each.name().equalsIgnoreCase(search)) {
                return each;
            }
        }
        return null;
    }
}
