package com.gel.driver.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringsUtil {
    public static boolean isNullOrEmpty(@Nullable String s) {
        return s == null || s.equals("");
    }

    public static @NotNull String padLeft(@NotNull String s, int count) {
        return padLeft(s, ' ', count);
    }
    public static @NotNull String padRight(@NotNull String s, int count) {
        return padRight(s, ' ', count);
    }

    public static @NotNull String padLeft(@NotNull String str, char character, int count) {
        StringBuilder s = new StringBuilder(str);

        while(s.length() < count) {
            s.insert(0, character);
        }

        return s.toString();
    }
    public static @NotNull String padRight(@NotNull String str, char character, int count) {
        StringBuilder s = new StringBuilder(str);

        while(s.length() < count) {
            s.append(character);
        }

        return s.toString();
    }
}
