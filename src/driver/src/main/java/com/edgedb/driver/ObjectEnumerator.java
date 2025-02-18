package com.edgedb.driver;

import com.edgedb.driver.exceptions.GelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.Map;

/**
 * Represents a handle to the deserialization pipeline of an object.
 */
public interface ObjectEnumerator {
    /**
     * Gets whether there are remaining elements to be enumerated.
     * @return {@code true} if there are remaining elements; otherwise {@code false}.
     */
    boolean hasRemaining();

    /**
     * Gets the next {@linkplain ObjectElement}, deserializing the value.
     * @return The next element.
     * @throws GelException A deserialization error occurred.
     * @throws OperationNotSupportedException The read operation isn't allowed.
     */
    @Nullable ObjectElement next() throws GelException, OperationNotSupportedException;

    /**
     * Flattens this enumerator into a single map, consuming the remaining data.
     * @return A map of name-value pairs.
     * @throws GelException A deserialization error occurred.
     * @throws OperationNotSupportedException The read operation isn't allowed.
     */
    Map<String, Object> flatten() throws GelException, OperationNotSupportedException;

    /**
     * Represents an element read from a {@linkplain ObjectEnumerator}.
     */
    final class ObjectElement {
        private final @NotNull String name;
        private final Object value;
        private final @NotNull Class<?> type;

        public ObjectElement(@NotNull String name, Object value, @NotNull Class<?> type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }

        public @NotNull String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        public @NotNull Class<?> getType() {
            return type;
        }
    }
}
