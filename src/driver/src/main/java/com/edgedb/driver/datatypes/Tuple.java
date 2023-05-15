package com.edgedb.driver.datatypes;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.binary.builders.ObjectEnumerator;
import com.edgedb.driver.exceptions.EdgeDBException;

import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@EdgeDBType
public final class Tuple {
    private final List<TupleElement> elements;

    @EdgeDBDeserializer
    public Tuple(ObjectEnumerator enumerator) throws EdgeDBException, OperationNotSupportedException {
        elements = new ArrayList<>();

        while(enumerator.hasRemaining()) {
            var enumerationElement = enumerator.next();

            assert enumerationElement != null;

            elements.add(new TupleElement(enumerationElement.type, enumerationElement.value));
        }
    }

    public Tuple(Object... values) {
        if(values == null) {
            this.elements = new ArrayList<>();
            return;
        }

        this.elements = Arrays.stream(values)
                .map(TupleElement::new)
                .collect(Collectors.toList());
    }

    public Tuple(TupleElement... elements) {
        this.elements = elements == null
                ? new ArrayList<>()
                : Arrays.asList(elements);
    }

    public boolean add(Object value) {
        return elements.add(new TupleElement(value));
    }

    public <T> boolean add(T value, Class<T> type) {
        return elements.add(new TupleElement(type, value));
    }

    public Object get(int index) {
        return this.getElement(index).value;
    }

    public TupleElement getElement(int index) {
        return this.elements.get(index);
    }

    public <T> T get(int index, Class<T> type) {
        var element = this.getElement(index);

        return elementAs(element, type);
    }

    public Object remove(int index) {
        return this.removeElement(index).value;
    }

    public TupleElement removeElement(int index) {
        return this.elements.remove(index);
    }

    public <T> T remove(int index, Class<T> type) {
        var removed = removeElement(index);

        return elementAs(removed, type);
    }

    public Object[] toValueArray() {
        return this.elements.stream().map(x -> x.value).toArray();
    }

    public TupleElement[] toArray() {
        var arr = new TupleElement[this.elements.size()];
        return this.elements.toArray(arr);
    }

    private <T> T elementAs(TupleElement element, Class<T> type) {
        if(!type.isAssignableFrom(element.type)) {
            throw new ClassCastException(String.format("Element type is not of %s. Actual: %s", type.getName(), element.type.getName()));
        }

        //noinspection unchecked
        return (T)element.value;
    }

    public static final class TupleElement {
        public final Class<?> type;
        public final Object value;

        public TupleElement(Class<?> type, Object value) {
            this.type = type;
            this.value = value;
        }

        public TupleElement(Object value) {
            this.type = value == null ? Object.class : value.getClass();
            this.value = value;
        }

        public static final TupleElement NULL_ELEMENT = new TupleElement(Object.class, null);
    }
}
