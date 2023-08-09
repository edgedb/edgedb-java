package com.edgedb.codegen.utils;

import java.io.File;
import java.nio.file.Path;

public class PathUtils {
    public static Path fromRelativeInput(String path) {
        if(path.startsWith("~" + File.separator)) {
            path = System.getProperty("user.home") + path.substring(1);
        } else if(path.equals("~")) {
            path = System.getProperty("user.home");
        }

        return Path.of(path);
    }
}
