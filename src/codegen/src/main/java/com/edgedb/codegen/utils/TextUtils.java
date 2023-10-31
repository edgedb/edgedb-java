package com.edgedb.codegen.utils;

import com.edgedb.driver.util.StringsUtil;

public class TextUtils {
    public static String nameWithoutModule(String name) {
        if(StringsUtil.isNullOrEmpty(name)) {
            return null;
        }

        return name.contains("::")
                ? name.split("::")[1]
                : name;
    }
}
