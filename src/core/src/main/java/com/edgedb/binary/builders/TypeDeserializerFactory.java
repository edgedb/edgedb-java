package com.edgedb.binary.builders;

import com.edgedb.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

@FunctionalInterface
public interface TypeDeserializerFactory<T> {
    T deserialize(
            final ObjectEnumerator enumerator,
            final @Nullable ParentDeserializer<T> parent
    ) throws EdgeDBException, OperationNotSupportedException, ReflectiveOperationException;

    default T deserialize(
            final ObjectEnumerator enumerator
    ) throws EdgeDBException, OperationNotSupportedException, ReflectiveOperationException {
        return deserialize(enumerator, null);
    }

    @FunctionalInterface
    interface ParentDeserializer<T> {
        void accept(T value, ObjectEnumerator.ObjectElement element) throws EdgeDBException, ReflectiveOperationException;
    }
}
