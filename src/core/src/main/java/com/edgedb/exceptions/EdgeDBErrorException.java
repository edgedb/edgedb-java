package com.edgedb.exceptions;

import com.edgedb.ErrorCode;
import com.edgedb.annotations.ShouldReconnect;
import com.edgedb.annotations.ShouldRetry;
import com.edgedb.binary.packets.receivable.ErrorResponse;
import com.edgedb.util.StringsUtil;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("ClassEscapesDefinedScope")
public class EdgeDBErrorException extends EdgeDBException {
    private static final short DETAILS_ATTRIBUTE = 0x0002;
    private static final short TRACEBACK_ATTRIBUTE = 0x0101;
    private static final short HINT_ATTRIBUTE = 0x0001;
    private static final short ERROR_LINE_START = (short)0xFFF3;
    private static final short ERROR_LINE_END = (short)0xFFF6;
    private static final short ERROR_COLUMN_START = (short)0xFFF5;
    private static final short ERROR_COLUMN_END = (short)0xFFF8;

    private final Map<Short, byte[]> attributes;
    private final ErrorCode errorCode;
    private final String message;

    private final @Nullable String query;

    public static EdgeDBErrorException fromError(ErrorResponse err) {
        return fromError(err, null);
    }
    public static EdgeDBErrorException fromError(ErrorResponse error, String query) {
        boolean shouldRetry, shouldReconnect;
        try {
            shouldRetry = ErrorCode.class.getField(error.errorCode.name()).isAnnotationPresent(ShouldRetry.class);
            shouldReconnect = ErrorCode.class.getField(error.errorCode.name()).isAnnotationPresent(ShouldReconnect.class);
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        return new EdgeDBErrorException(error, query, shouldRetry, shouldReconnect);
    }


    private EdgeDBErrorException(ErrorResponse error, @Nullable String query, boolean shouldRetry, boolean shouldReconnect) {
        super(shouldRetry, shouldReconnect);

        this.attributes = Arrays.stream(error.attributes)
                .collect(Collectors.toMap((v) -> v.code, (v) -> {
                    var arr = new byte[v.value.readableBytes()];
                    v.value.readBytes(arr);
                    return arr;
                }));

        this.errorCode = error.errorCode;
        this.message = error.message;
        this.query = query;
    }

    public String getErrorMessage() {
        return this.message;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public @Nullable String getDetails() {
        return getAttributeString(DETAILS_ATTRIBUTE);
    }

    public @Nullable String getTraceback() {
        return getAttributeString(TRACEBACK_ATTRIBUTE);
    }

    public @Nullable String getHint() {
        return getAttributeString(HINT_ATTRIBUTE);
    }

    public @Nullable String getQuery() {
        return this.query;
    }

    private @Nullable String getAttributeString(short code) {
        return getAttributeData(code, (b) -> new String(b, StandardCharsets.UTF_8));
    }

    private <T> @Nullable T getAttributeData(short code, Function<byte[], T> mapper) {
        if(attributes.containsKey(code)) {
            return mapper.apply(attributes.get(code));
        }

        return null;
    }

    @Override
    public String toString() {
        var pretty = prettify();

        if(pretty != null) {
            return pretty;
        }

        return String.format("%s: %s", this.errorCode, this.message);
    }

    private String prettify() {
        String lineStartStr, lineEndStr, columnStartStr, columnEndStr;
        
        if(
                this.query == null ||
                (lineStartStr = getAttributeString(ERROR_LINE_START)) == null ||
                (lineEndStr = getAttributeString(ERROR_LINE_END)) == null ||
                (columnStartStr = getAttributeString(ERROR_COLUMN_START)) == null ||
                (columnEndStr = getAttributeString(ERROR_COLUMN_END)) == null
        ) {
            return null;
        }

        var lines = this.query.split("\n");
        var lineNoWidth = lineEndStr.length();

        var errorMessage = new StringBuilder(String.format("%s: %s\n", this.errorCode, this.message));
        errorMessage.append(StringsUtil.padLeft("|", lineNoWidth + 3)).append("\n");

        int
                lineStart = Integer.parseInt(lineStartStr),
                lineEnd   = Integer.parseInt(lineEndStr),
                colStart  = Integer.parseInt(columnStartStr),
                colEnd    = Integer.parseInt(columnEndStr);

        for(int i = lineStart; i < lineEnd + 1; i++) {
            var line = lines[i - 1];
            var start = i == lineStart ? colStart : 0;
            var end = i == lineEnd ? colEnd : line.length();

            errorMessage.append(" ").append(StringsUtil.padLeft(Integer.toString(i), lineNoWidth)).append(" | ").append(line).append("\n");
            errorMessage.append(StringsUtil.padLeft("|", lineNoWidth + 3)).append(" ").append(StringsUtil.padLeft(StringsUtil.padLeft("", '^', end - start), end)).append("\n");
        }

        if(getHint() != null) {
            errorMessage.append("Hint: ").append(getHint());
        }

        return errorMessage.toString();
    }
}
