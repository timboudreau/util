/*
 * The MIT License
 *
 * Copyright 2019 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.abstractions.list;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;

/**
 *
 * @author Tim Boudreau
 */
final class ArrayIndexed<T> implements IndexedResolvable<T> {

    private final T[] sortedArray;

    ArrayIndexed(T[] sortedArray) {
        this.sortedArray = sortedArray;
    }

    @SuppressWarnings("unchecked")
    ArrayIndexed(Class<T> type, Collection<? extends T> coll, Comparator<? super T> comp) {
        T[] objs = (T[]) Array.newInstance(type, coll.size());
        sortedArray = coll.toArray(objs);
        if (!(coll instanceof SortedSet<?>)) {
            Arrays.sort(objs, comp);
        }
    }

    static <T extends Comparable<T>> ArrayIndexed<T> fromCollection(Collection<? extends T> coll, Class<T> type) {
        @SuppressWarnings("Convert2Lambda")
        Comparator<? super T> comp = new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return o1.compareTo(o2);
            }
        };
        return new ArrayIndexed<>(type, coll, comp);
    }

    static <T extends Comparable<? super T>> ArrayIndexed<T> fromArray(T[] array) {
        if (isSorted(array)) {
            return new ArrayIndexed<>(array);
        } else {
            T[] nue = Arrays.copyOf(array, array.length);
            return new ArrayIndexed<>(nue);
        }
    }

    static <T extends Comparable<? super T>> ArrayIndexed<T> create(Class<T> type, Collection<? extends T> coll) {
        return new ArrayIndexed<>(type, coll, Comparable::compareTo);
    }

    static <T extends Comparable<? super T>> boolean isSorted(T[] array) {
        T last = null;
        for (int i = 0; i < array.length; i++) {
            T object = array[i];
            if (last != null) {
                int ix = last.compareTo(object);
                if (ix < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public T forIndex(int index) {
        return sortedArray[index];
    }

    @Override
    public int size() {
        return sortedArray.length;
    }

    @Override
    public int indexOf(Object obj) {
        if (!sortedArray.getClass().getComponentType().isInstance(obj)) {
            return -1;
        }
        return Arrays.binarySearch(sortedArray, obj);
    }
}
