package com.edgedb.driver.datatypes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A class representing the {@code std::range} type.
 * @param <T> The inner type of the range.
 * @see <a href="https://www.edgedb.com/docs/stdlib/range">EdgeDB range docs</a>
 */
public final class Range<T> {
    /**
     * An empty range with no specific inner type.
     */
    public static final Range<?> EMPTY_RANGE = new Range<>(Long.class, null, null);

    private static List<Class<?>> VALID_ELEMENTS;

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
        checkValidElement(cls);

        this.lower = lower;
        this.upper = upper;
        this.includeLower = includeLower;
        this.includeUpper = includeUpper;
        this.isEmpty = (lower == null && upper == null) || (lower != null && lower.equals(upper));
        this.elementType = cls;
    }

    private synchronized void checkValidElement(Class<?> cls) {
        var valid = VALID_ELEMENTS == null ? (VALID_ELEMENTS = new ArrayList<>(){{
            add(Integer.class);
            add(Integer.TYPE);
            add(Long.class);
            add(Long.TYPE);
            add(Float.class);
            add(Float.TYPE);
            add(Double.class);
            add(Double.TYPE);
            add(BigDecimal.class);
            add(OffsetDateTime.class);
            add(ZonedDateTime.class);
            add(LocalDateTime.class);
            add(LocalDate.class);
        }}) : VALID_ELEMENTS;

        if(!valid.contains(cls)) {
            throw new IllegalArgumentException("Range element type is invalid");
        }
    }

    /**
     * Gets the lower bounds of the range.
     * @return The lower bounds if present; otherwise {@code null}.
     */
    public @Nullable T getLower() {
        return this.lower;
    }

    /**
     * Gets the upper bounds of the range.
     * @return The upper bounds if present; otherwise {@code null}.
     */
    public @Nullable T getUpper() {
        return this.upper;
    }

    /**
     * Gets whether this range includes a lower bounds.
     * @return {@code true} if this range includes a lower bounds; otherwise {@code false}.
     */
    public boolean doesIncludeLower() {
        return this.includeLower;
    }

    /**
     * Gets whether this range includes an upper bounds.
     * @return {@code true} if this range includes an upper bounds; otherwise {@code false}.
     */
    public boolean doesIncludeUpper() {
        return this.includeUpper;
    }

    /**
     * Gets whether this range is empty.
     * @return {@code true} if this range is empty; otherwise {@code false}.
     */
    public boolean isEmpty() {
        return this.isEmpty;
    }

    /**
     * Gets an empty range with the element type {@linkplain U}
     * @return An empty range of type {@linkplain U}.
     * @param <U> The element type of the range.
     */
    @SuppressWarnings("unchecked")
    public static <U> Range<U> empty() {
        return (Range<U>)EMPTY_RANGE;
    }

    /**
     * Creates an empty range with the element type {@linkplain U}.
     * @param cls The element types class.
     * @return An empty range of type {@linkplain U}.
     * @param <U> The element type of the range.
     */
    public static <U> @NotNull Range<U> empty(Class<U> cls) {
        return new Range<>(cls, null, null);
    }

    /**
     * Creates a new {@linkplain Range<U>}.
     * @param cls The element class of the new range.
     * @param lower The lower bounds of the range.
     * @param upper The upper bounds of the range.
     * @return A new {@linkplain Range<U>} of {@linkplain U}.
     * @param <U> The element type of the range.
     */
    public static <U> @NotNull Range<U> create(Class<U> cls, @Nullable U lower, @Nullable U upper) {
        return new Range<>(cls, lower, upper);
    }

    /**
     * Creates a new {@linkplain Range<U>}.
     * @param cls The element class of the new range.
     * @param lower The lower bounds of the range.
     * @param upper The upper bounds of the range.
     * @param includeLower Whether to include the lower bounds of the range.
     * @param includeUpper Whether to include the upper bounds of the range.
     * @return A new {@linkplain Range<U>} of {@linkplain U}.
     * @param <U> The element type of the range.
     */
    public static <U> @NotNull Range<U> create(
            Class<U> cls,
            @Nullable U lower,
            @Nullable U upper,
            boolean includeLower,
            boolean includeUpper
    ) {
        return new Range<>(cls, lower, upper, includeLower, includeUpper);
    }

    /**
     * Gets a {@linkplain Class} that represents a range of a provided type.
     * @param cls The inner type of the range.
     * @return A class that represents a range of a provide type.
     * @param <U> The inner type of the range.
     */
    @SuppressWarnings("unchecked")
    public static <U> Class<Range<U>> ofType(Class<U> cls) {
        return (Class<Range<U>>) EMPTY_RANGE.getClass();
    }

    /**
     * Gets the element types class of this range.
     * @return The {@linkplain Class<T>} of the element type.
     */
    public Class<T> getElementType() {
        return this.elementType;
    }

    @Override
    public boolean equals(@Nullable Object o) {
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
