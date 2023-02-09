package com.edgedb.driver.util;

public class StringsUtil {
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.equals("");
    }
}
