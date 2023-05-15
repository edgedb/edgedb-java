package com.edgedb.driver.binary.builders;

import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.Map;

public interface ObjectEnumerator {
    boolean hasRemaining();
    @Nullable ObjectElement next() throws EdgeDBException, OperationNotSupportedException;

    Map<String, Object> flatten() throws EdgeDBException, OperationNotSupportedException;

    final class ObjectElement {
        public final String name;
        public final Object value;
        public final Class<?> type;

        public ObjectElement(String name, Object value, Class<?> type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }
    }
}
