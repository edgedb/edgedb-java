package com.gel.driver.datatypes;

import com.gel.driver.datatypes.internal.TupleImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Represents a basic tuple type storing elements with type information.
 */
public interface Tuple {
    /**
     * Adds a value to the end of this tuple.
     * @param value The value to add.
     * @return {@code true} if the value was successfully added; otherwise {@code false}.
     */
    boolean add(Object value);

    /**
     * Adds a value to the end of this tuple.
     * @param value The value to add.
     * @param type The type of the value.
     * @return {@code true} if the value was successfully added; otherwise {@code false}.
     * @param <T> The type of the value.
     */
    <T> boolean add(T value, Class<T> type);

    /**
     * Gets a value at the specified index.
     * @param index The index of the value to get.
     * @return The value at the specified index.
     * @throws IndexOutOfBoundsException The index exceeds the size of the tuple.
     */
    @Nullable Object get(int index);

    /**
     * Gets a {@linkplain Element} at the specified index.
     * @param index The index of the element to get.
     * @return The element at the specified index.
     * @throws IndexOutOfBoundsException The index exceeds the size of the tuple.
     */
    Element getElement(int index);

    /**
     * Gets a value at the specified index, as the specified type.
     * @param index The index of the value to get.
     * @param type The type of the value.
     * @return The value at the specified index.
     * @param <T> The type of the value.
     * @throws ClassCastException The value at the specified index is not the type supplied.
     * @throws IndexOutOfBoundsException The index exceeds the size of the tuple.
     */
    <T> @Nullable T get(int index, Class<T> type);

    /**
     * Removes a value at the specified index.
     * @param index The index of the value to remove.
     * @return The removed value.
     * @throws IndexOutOfBoundsException The index exceeds the size of the tuple.
     */
    @Nullable Object remove(int index);

    /**
     * Removes an element at the specified index.
     * @param index The index of the value to remove.
     * @return The removed value.
     * @throws IndexOutOfBoundsException The index exceeds the size of the tuple.
     */
    Element removeElement(int index);

    /**
     * Removes a value at the specified index.
     * @param index The index of the value to remove.
     * @param type The type of the value at the index.
     * @return The value as the specified type, at the specified index.
     * @param <T> The type of the value.
     * @throws ClassCastException The value at the specified index is not the type supplied.
     * @throws IndexOutOfBoundsException The index exceeds the size of the tuple.
     */
    <T> @Nullable T remove(int index, Class<T> type);

    /**
     * Turns the current tuple into a value array.
     * @return An array of all the values in this tuple, preserving the arity of the tuple.
     */
    Object[] toArray();

    /**
     * Turns the current tuple into an {@linkplain Element} array.
     * @return An array of all the elements in this tuple, preserving the arity of the tuple.
     */
    Element[] toElementArray();

    /**
     * Gets the number of elements within this tuple.
     * @return The number of elements this tuple contains.
     */
    int size();

    /**
     * Creates a tuple with the specified element values.
     * @param values The element values for the new tuple.
     * @return A tuple with all the values of the supplied array, maintaining arity.
     */
    static @NotNull Tuple of(Object... values) {
        return new TupleImpl(values);
    }

    /**
     * Creates a tuple with the specified elements.
     * @param elements The elements for the new tuple.
     * @return A tuple with all the elements of the supplied array, maintaining arity.
     */
    static @NotNull Tuple of(Element... elements) {
        return new TupleImpl(elements);
    }

    /**
     * Creates a tuple with the specified elements.
     * <br/><br/>
     * {@linkplain Element}s within the collection will be added without modification, while other values will be
     * converted to a {@linkplain Element} using {@linkplain Element#of(Object)}
     * @param elements A collection of elements for the new tuple.
     * @return A tuple with all the elements of the supplied collection, maintaining arity.
     */
    static @NotNull Tuple of(Collection<?> elements) {
        return new TupleImpl(elements);
    }

    /**
     * Creates an empty tuple, equivalent of calling {@linkplain Tuple#of(Object...)} with no parameters.
     * @return An empty tuple.
     */
    static @NotNull Tuple empty() {
        return new TupleImpl();
    }

    /**
     * Represents an element within a {@linkplain Tuple}.
     */
    interface Element {
        /**
         * Gets the type of this element, relating to its value.
         * @return The type of this elements value.
         */
        Class<?> getType();

        /**
         * Gets the value of this element.
         * @return The value of this element.
         */
        @Nullable Object getValue();

        /**
         * Creates a new element with the specified value.
         * @param value The value for the element.
         * @return A {@linkplain Element} containing the supplied value.
         */
        static @NotNull Element of(@Nullable Object value) {
            return new TupleImpl.ElementImpl(value);
        }

        /**
         * Creates a new element with the specified value and type.
         * @param value The value for the element.
         * @param type The type for the element.
         * @return A {@linkplain Element} containing the supplied value and type.
         */
        static @NotNull Element of(@Nullable Object value, Class<?> type) {
            return new TupleImpl.ElementImpl(type, value);
        }
    }
}
