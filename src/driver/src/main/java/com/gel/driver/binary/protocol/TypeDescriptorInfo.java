package com.gel.driver.binary.protocol;

import java.util.UUID;

public class TypeDescriptorInfo<T extends Enum<T>> {
    public final TypeDescriptor descriptor;
    public final T type;

    public TypeDescriptorInfo(TypeDescriptor descriptor, T type) {
        this.descriptor = descriptor;
        this.type = type;
    }

    public UUID getId() {
        return descriptor.getId();
    }

    @SuppressWarnings("unchecked")
    public <U extends TypeDescriptor> U as(Class<U> ignored) {
        return (U)descriptor;
    }
}
