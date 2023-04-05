package com.edgedb.util;

public class StringsUtil {
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.equals("");
    }

    public static String padLeft(String s, int count) {
        return padLeft(s, ' ', count);
    }
    public static String padRight(String s, int count) {
        return padRight(s, ' ', count);
    }

    public static String padLeft(String str, char character, int count) {
        StringBuilder s = new StringBuilder(str);

        while(s.length() < count) {
            s.insert(0, character);
        }

        return s.toString();
    }
    public static String padRight(String str, char character, int count) {
        StringBuilder s = new StringBuilder(str);

        while(s.length() < count) {
            s.append(character);
        }

        return s.toString();
    }
}
