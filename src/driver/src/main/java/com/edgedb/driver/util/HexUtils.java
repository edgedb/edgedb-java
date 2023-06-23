package com.edgedb.driver.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class HexUtils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static @NotNull String bufferToHexString(@NotNull ByteBuffer buffer) {

        char[] hexChars = new char[buffer.limit() * 2];
        int j =0;
        while(buffer.hasRemaining()) {
            int v = buffer.get() & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
            j++;
        }
        return new String(hexChars);
    }

    public static @NotNull String bufferToHexString(@NotNull ByteBuf buffer) {
        return ByteBufUtil.hexDump(buffer);
    }

    public static @NotNull String byteArrayToHexString(byte @NotNull [] b) {
        StringBuilder result = new StringBuilder();
        for (byte value : b) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    public static byte[] hexStringToByteArray(@NotNull String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
