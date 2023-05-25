package com.edgedb.driver.exceptions;

import com.edgedb.driver.ErrorCode;
import com.edgedb.driver.util.StringsUtil;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents an exception that was caused by an error from EdgeDB.
 */
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

    /**
     * Constructs a new {@linkplain EdgeDBErrorException}.
     * @param attributes The attributes of the error, received by EdgeDB.
     * @param message The error message.
     * @param errorCode The error code.
     * @param query The optional query that caused this error.
     */
    public EdgeDBErrorException(Map<Short, byte[]> attributes, String message, ErrorCode errorCode, @Nullable String query) {
        super(errorCode.shouldRetry(), errorCode.shouldReconnect());

        this.attributes = attributes;
        this.errorCode = errorCode;
        this.message = message;
        this.query = query;
    }

    /**
     * Gets the error message of this exception.
     * @return The error message returned from EdgeDB.
     */
    public String getErrorMessage() {
        return this.message;
    }

    /**
     * Gets the error code of this exception.
     * @return The error code returned by EdgeDB.
     */
    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    /**
     * Gets the details of this exception.
     * @return The details of this error returned by EdgeDB if present; otherwise {@code null}.
     */
    public @Nullable String getDetails() {
        return getAttributeString(DETAILS_ATTRIBUTE);
    }

    /**
     * Gets the EdgeDB traceback of this exception.
     * @return The traceback of this error returned by EdgeDB if present; otherwise {@code null}.
     */
    public @Nullable String getTraceback() {
        return getAttributeString(TRACEBACK_ATTRIBUTE);
    }

    /**
     * Gets the hint of this exception.
     * @return The hint of this error returned by EdgeDB if present; otherwise {@code null}.
     */
    public @Nullable String getHint() {
        return getAttributeString(HINT_ATTRIBUTE);
    }

    /**
     * Gets the query that was being executed as this error was thrown.
     * @return The query that was executed if present; otherwise {@code null}.
     */
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

    /**
     * Formats this exception into a detailed format outlining where in the query the error was caused if this
     * errors query is present, or formats the error code and message.
     * @return A string representing the current exception.
     */
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
