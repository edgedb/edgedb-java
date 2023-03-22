package com.edgedb.driver.datatypes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class Range<T extends Number> implements Comparable<Range<T>> {
    public static final Range<?> EMPTY_RANGE = new Range<>(null, null);

    private final @Nullable T lower;
    private final @Nullable T upper;
    private final boolean includeLower;
    private final boolean includeUpper;
    private final boolean isEmpty;

    public Range(@Nullable T lower, @Nullable T upper) {
        this(lower, upper, true, false);
    }

    public Range(@Nullable T lower, @Nullable T upper, boolean includeLower, boolean includeUpper) {
        this.lower = lower;
        this.upper = upper;
        this.includeLower = includeLower;
        this.includeUpper = includeUpper;
        this.isEmpty = (lower == null && upper == null) || (lower != null && lower.equals(upper));
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
    public static <U extends Number> Range<U> empty() {
        return (Range<U>)EMPTY_RANGE;
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
