package com.edgedb.driver.binary.builders;

@FunctionalInterface
public interface TypeDeserializerFactory<T> {
    T deserialize(final ObjectEnumerator enumerator);
}
