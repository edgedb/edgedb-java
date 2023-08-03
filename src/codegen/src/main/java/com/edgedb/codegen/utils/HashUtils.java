package com.edgedb.codegen.utils;

import com.edgedb.driver.util.HexUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtils {
    public static String hashEdgeQL(String edgeql) throws NoSuchAlgorithmException {
        var digest = MessageDigest.getInstance("SHA-256");
        return HexUtils.byteArrayToHexString(digest.digest(edgeql.getBytes(StandardCharsets.UTF_8)));
    }
}
