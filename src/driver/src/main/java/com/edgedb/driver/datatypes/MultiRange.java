package com.edgedb.driver.datatypes;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * Represents the {@code multirange} type in EdgeDB
 * @param <T> The inner type of the multirange.
 */
@SuppressWarnings("unchecked")
public class MultiRange<T> {
    private static final Range<?>[] EMPTY_RANGE_ARRAY = new Range<?>[0];
    private static final MultiRange<?> EMPTY_MULTI_RANGE = new MultiRange<>();

    /**
     * Gets the length of this multirange.
     */
    public final int length;

    private final Range<T>[] ranges;

    /**
     * Constructs a new empty multirange
     */
    public MultiRange() {
        ranges = (Range<T>[]) EMPTY_RANGE_ARRAY;
        length = 0;
    }

    /**
     * Constructs a new multirange with the provided elements.
     * @param elements The elements to construct the multirange with.
     */
    public MultiRange(Collection<? extends Range<T>> elements) {
        ranges = elements.toArray((Range<T>[])EMPTY_RANGE_ARRAY);
        length = ranges.length;
    }

    public MultiRange(Range<T>[] elements) {
        ranges = elements.clone();
        length = elements.length;
    }


    /**
     * Gets an element within this multirange by index.
     * @param i The index of the element to get.
     * @return The element at the specified index.
     */
    public Range<T> get(int i) throws IndexOutOfBoundsException {
        return ranges[i];
    }

    /**
     * Converts this multirange into a hashset.
     * @return A hashset representing this multirange.
     */
    public HashSet<Range<T>> toSet() {
        return new HashSet<>(Arrays.asList(ranges));
    }

    public static <U> MultiRange<U> empty(Class<U> cls) {
        return new MultiRange<>();
    }

    public static <U> MultiRange<U> empty() {
        return (MultiRange<U>) EMPTY_MULTI_RANGE;
    }

    /**
     * Gets a {@linkplain Class} that represents a multirange of a specified type.
     * @param cls The inner type of the multirange to represent.
     * @return A class that represents a multirange of the provided type.
     * @param <U> The inner type of the multirange.
     */
    public static <U> Class<MultiRange<U>> ofType(Class<U> cls) {
        return (Class<MultiRange<U>>) EMPTY_MULTI_RANGE.getClass();
    }
}
