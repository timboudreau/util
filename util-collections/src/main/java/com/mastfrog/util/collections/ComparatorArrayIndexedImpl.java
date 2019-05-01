/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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

package com.mastfrog.util.collections;

import com.mastfrog.abstractions.list.IndexedResolvable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
final class ComparatorArrayIndexedImpl<T> implements IndexedResolvable<T> {

    private final T[] sorted;
    private final int[] indices;
    private final T[] origOrder;

    ComparatorArrayIndexedImpl(Comparator<T> compar, T... items) {
        origOrder = items;
        sorted = Arrays.copyOf(items, items.length);
        Arrays.sort(sorted, compar);
        indices = new int[items.length];
        List<T> order = Arrays.asList(items);
        CollectionUtils.checkDuplicates(order);
        for (int i = 0; i < items.length; i++) {
            T s = sorted[i];
            indices[i] = order.indexOf(s);
        }
    }

    @Override
    public int size() {
        return sorted.length;
    }

    @Override
    public int indexOf(Object o) {
        int ix = Arrays.binarySearch(sorted, o);
        return ix < 0 ? -1 : indices[ix];
    }

    public T get(int index) {
        return origOrder[index];
    }

    @Override
    public T forIndex(int index) {
        return get(index);
    }

}
