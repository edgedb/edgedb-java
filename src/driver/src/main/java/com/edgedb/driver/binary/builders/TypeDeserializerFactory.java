package com.edgedb.driver.binary.builders;

import com.edgedb.driver.ObjectEnumerator;
import com.edgedb.driver.exceptions.GelException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

@FunctionalInterface
public interface TypeDeserializerFactory<T> {
    T deserialize(
            final ObjectEnumerator enumerator,
            final @Nullable ParentDeserializer<T> parent
    ) throws GelException, OperationNotSupportedException, ReflectiveOperationException;

    default T deserialize(
            final ObjectEnumerator enumerator
    ) throws GelException, OperationNotSupportedException, ReflectiveOperationException {
        return deserialize(enumerator, null);
    }

    @FunctionalInterface
    interface ParentDeserializer<T> {
        void accept(T value, ObjectEnumerator.ObjectElement element) throws GelException, ReflectiveOperationException;
    }
}
