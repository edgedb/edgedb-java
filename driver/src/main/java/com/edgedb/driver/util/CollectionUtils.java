package com.edgedb.driver.util;

import java.util.List;
import java.util.NoSuchElementException;

public class CollectionUtils {
    public static <T, E extends List<T>> T last(E list) {
        if (list.isEmpty()) {
            throw new NoSuchElementException();
        }

        return list.get(list.size() - 1);
    }

    public static <T> T last(T[] arr) {
        if (arr.length == 0) {
            throw new NoSuchElementException();
        }

        return arr[arr.length-1];
    }
}
