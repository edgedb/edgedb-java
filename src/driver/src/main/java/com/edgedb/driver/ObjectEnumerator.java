package com.edgedb.driver;

import com.edgedb.driver.exceptions.EdgeDBException;
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
     * @throws EdgeDBException A deserialization error occurred.
     * @throws OperationNotSupportedException The read operation isn't allowed.
     */
    @Nullable ObjectElement next() throws EdgeDBException, OperationNotSupportedException;

    /**
     * Flattens this enumerator into a single map, consuming the remaining data.
     * @return A map of name-value pairs.
     * @throws EdgeDBException A deserialization error occurred.
     * @throws OperationNotSupportedException The read operation isn't allowed.
     */
    Map<String, Object> flatten() throws EdgeDBException, OperationNotSupportedException;

    /**
     * Represents an element read from a {@linkplain ObjectEnumerator}.
     */
    final class ObjectElement {
        private final String name;
        private final Object value;
        private final Class<?> type;

        public ObjectElement(String name, Object value, Class<?> type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        public Class<?> getType() {
            return type;
        }
    }
}
