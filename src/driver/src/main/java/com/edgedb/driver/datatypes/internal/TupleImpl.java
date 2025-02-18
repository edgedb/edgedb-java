package com.edgedb.driver.datatypes.internal;

import com.edgedb.driver.annotations.GelDeserializer;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.ObjectEnumerator;
import com.edgedb.driver.datatypes.Tuple;
import com.edgedb.driver.exceptions.GelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.*;
import java.util.stream.Collectors;

@EdgeDBType
public final class TupleImpl implements Tuple {
    private final @NotNull List<Element> elements;

    @GelDeserializer
    public TupleImpl(@NotNull ObjectEnumerator enumerator) throws GelException, OperationNotSupportedException {
        elements = new ArrayList<>();

        while(enumerator.hasRemaining()) {
            var enumerationElement = enumerator.next();

            assert enumerationElement != null;

            elements.add(new ElementImpl(enumerationElement.getType(), enumerationElement.getValue()));
        }
    }

    public TupleImpl(Object @Nullable ... values) {
        if(values == null) {
            this.elements = new ArrayList<>();
            return;
        }

        this.elements = Arrays.stream(values)
                .map(ElementImpl::new)
                .collect(Collectors.toList());
    }

    public TupleImpl(@Nullable Collection<?> values) {
        if(values == null) {
            this.elements = new ArrayList<>();
            return;
        }

        this.elements = values.stream()
                .map(v -> v instanceof Element ? (Element) v : new ElementImpl(v))
                .collect(Collectors.toList());
    }

    @SafeVarargs
    public <T extends Element> TupleImpl(T @Nullable ... elements) {
        this.elements = elements == null
                ? new ArrayList<>()
                : Arrays.asList(elements);
    }

    public boolean add(Object value) {
        return elements.add(new ElementImpl(value));
    }

    @Override
    public <T> boolean add(T value, Class<T> type) {
        return elements.add(new ElementImpl(type, value));
    }

    @Override
    public @Nullable Object get(int index) {
        return this.getElement(index).getValue();
    }

    @Override
    public Element getElement(int index) {
        return this.elements.get(index);
    }

    @Override
    public <T> @Nullable T get(int index, @NotNull Class<T> type) {
        var element = this.getElement(index);

        return elementAs(element, type);
    }

    @Override
    public @Nullable Object remove(int index) {
        return this.removeElement(index).getValue();
    }

    @Override
    public Element removeElement(int index) {
        return this.elements.remove(index);
    }

    @Override
    public <T> @Nullable T remove(int index, @NotNull Class<T> type) {
        var removed = removeElement(index);

        return elementAs(removed, type);
    }

    @Override
    public Object @NotNull [] toArray() {
        return this.elements.stream().map(Element::getValue).toArray();
    }

    @Override
    public Element @NotNull [] toElementArray() {
        var arr = new Element[this.elements.size()];
        return this.elements.toArray(arr);
    }

    private <T> @Nullable T elementAs(@NotNull Element element, @NotNull Class<T> type) {
        if(!type.isAssignableFrom(element.getType())) {
            throw new ClassCastException(String.format("Element type is not of %s. Actual: %s", type.getName(), element.getType().getName()));
        }

        //noinspection unchecked
        return (T)element.getValue();
    }

    @Override
    public int size() {
        return elements.size();
    }

    public static final class ElementImpl implements Element {
        public final Class<?> type;
        public final @Nullable Object value;

        public ElementImpl(Class<?> type, @Nullable Object value) {
            this.type = type;
            this.value = value;
        }

        public ElementImpl(@Nullable Object value) {
            this.type = value == null ? Object.class : value.getClass();
            this.value = value;
        }

        @Override
        public Class<?> getType() {
            return type;
        }

        @Override
        public @Nullable Object getValue() {
            return value;
        }
    }
}