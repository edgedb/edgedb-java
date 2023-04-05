package com.edgedb.datatypes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class Range<T> implements Comparable<Range<T>> {
    public static final Range<?> EMPTY_RANGE = new Range<>(Long.class, null, null);

    private final @Nullable T lower;
    private final @Nullable T upper;
    private final boolean includeLower;
    private final boolean includeUpper;
    private final boolean isEmpty;

    private final Class<T> elementType;

    private Range(Class<T> cls, @Nullable T lower, @Nullable T upper) {
        this(cls, lower, upper, true, false);
    }

    private Range(Class<T> cls, @Nullable T lower, @Nullable T upper, boolean includeLower, boolean includeUpper) {
        this.lower = lower;
        this.upper = upper;
        this.includeLower = includeLower;
        this.includeUpper = includeUpper;
        this.isEmpty = (lower == null && upper == null) || (lower != null && lower.equals(upper));
        this.elementType = cls;
    }

    public @Nullable T getLower() {
        return this.lower;
    }

    public @Nullable T getUpper() {
        return this.upper;
    }

    public boolean doesIncludeLower() {
        return this.includeLower;
    }

    public boolean doesIncludeUpper() {
        return this.includeUpper;
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    @SuppressWarnings("unchecked")
    public static <U> Range<U> empty() {
        return (Range<U>)EMPTY_RANGE;
    }
    public static <U> Range<U> empty(Class<U> cls) {
        return new Range<>(cls, null, null);
    }

    public static <U> Range<U> create(Class<U> cls, @Nullable U lower, @Nullable U upper) {
        return new Range<>(cls, lower, upper);
    }

    public static <U> Range<U> create(
            Class<U> cls,
            @Nullable U lower,
            @Nullable U upper,
            boolean includeLower,
            boolean includeUpper
    ) {
        return new Range<>(cls, lower, upper, includeLower, includeUpper);
    }

    public Class<T> getElementType() {
        return this.elementType;
    }

    @Override
    public int compareTo(@NotNull Range<T> o) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Range<?> range = (Range<?>) o;
        return
                includeLower == range.includeLower &&
                includeUpper == range.includeUpper &&
                isEmpty == range.isEmpty &&
                Objects.equals(lower, range.lower) &&
                Objects.equals(upper, range.upper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lower, upper, includeLower, includeUpper, isEmpty);
    }
}
