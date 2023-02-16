package com.edgedb.driver.exceptions;

import com.edgedb.driver.ErrorCode;
import com.edgedb.driver.annotations.ShouldReconnect;
import com.edgedb.driver.annotations.ShouldRetry;
import com.edgedb.driver.binary.packets.receivable.ErrorResponse;
import org.jetbrains.annotations.NotNull;
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

    private @Nullable String query;


    public EdgeDBErrorException(ErrorResponse error) throws NoSuchFieldException {
        super(
                ErrorCode.class.getField(error.errorCode.name()).isAnnotationPresent(ShouldRetry.class),
                ErrorCode.class.getField(error.errorCode.name()).isAnnotationPresent(ShouldReconnect.class)
        );

        this.attributes = Arrays.stream(error.attributes)
                .collect(Collectors.toMap((v) -> v.code, (v) -> v.value.array()));

        this.errorCode = error.errorCode;
        this.message = error.message;
    }

    public EdgeDBErrorException(ErrorResponse error, @NotNull String query) throws NoSuchFieldException {
        this(error);

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
        String lineStart, lineEnd, columnStart, columnEnd;
        
        if(
                this.query == null ||
                (lineStart = getAttributeString(ERROR_LINE_START)) == null ||
                (lineEnd = getAttributeString(ERROR_LINE_END)) == null ||
                (columnStart = getAttributeString(ERROR_COLUMN_START)) == null ||
                (columnEnd = getAttributeString(ERROR_COLUMN_END)) == null
        ) {
            return null;
        }

        var lines = this.query.split("\n");
        var lineNoWidth = lineEnd.length();

        var errorMessage = String.format("%s: %s", this.errorCode, this.message);
        errorMessage += "|".
    }
}
