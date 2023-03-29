package com.edgedb.driver.binary.builders;

import com.edgedb.driver.exceptions.EdgeDBException;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.InvocationTargetException;

@FunctionalInterface
public interface TypeDeserializerFactory<T> {
    T deserialize(final ObjectEnumerator enumerator) throws EdgeDBException, OperationNotSupportedException, ReflectiveOperationException;
}
