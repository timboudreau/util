/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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

import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * Base interface for the read-only portion of IntSet, IntList and possibly
 * others.
 *
 * @author Tim Boudreau
 */
public interface OrderedIntegerCollection extends Iterable<Integer> {

    /**
     * Get the number of elements in this collection.
     *
     * @return The numebr of elements
     */
    int size();

    /**
     * Get the value at a given index.
     *
     * @param position The position
     * @return The value at the position
     * @throws IndexOutOfBoundsException if the position is outside the bounds
     * of the size of this collection
     */
    int valueAt(int position);

    /**
     * Determine if the collection is empty.
     *
     * @return True if the size is zero
     */
    boolean isEmpty();

    /**
     * Determine if a value is present.
     *
     * @param value A value
     * @return True if it is present, false otherwise
     */
    boolean contains(int value);

    /**
     * Get the contents copied into an integer array.
     *
     * @return
     */
    int[] toIntArray();

    /**
     * Visit each value in order; most implementations also have a
     * <code>forEach()</code> method that can take an <code>IntConsumer</code>;
     * or a <code>Consumer&lt;Integer&gt;</code>; this method enables such code
     * to be written with a lambda that does not either need an explicit
     * parameter type or to be cast a <code>IntConsumer</code> to avoid the
     * overhead of auto-boxing.
     *
     * @param consumer A consumer
     */
    void forEachInt(IntConsumer consumer);

    /**
     * Overloads <code>iterator()</code> to return a primitive iterator.
     *
     * @return An iterator
     */
    @Override
    PrimitiveIterator.OfInt iterator();

    /**
     * Get the first element.
     *
     * @return the return value of <code>valueAt(0)</code>
     * @throws IndexOutOfBoundsException if empty
     */
    default int first() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("Empty");
        }
        return valueAt(0);
    }

    /**
     * Get the first element.
     *
     * @return the return value of <code>valueAt(size()-1)</code>
     * @throws IndexOutOfBoundsException if empty
     */
    default int last() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("Empty");
        }
        return valueAt(size() - 1);
    }

    /**
     * Iterate the contents in reverse order.
     *
     * @param consumer A consumer
     */
    default void forEachReversed(IntConsumer consumer) {
        int sz = size();
        for (int i = sz; i >= 0; i--) {
            consumer.accept(valueAt(i));
        }
    }

    default String toString(String delimiter) {
        return toString(delimiter, Integer::toString);
    }

    default String toString(String delimiter, IntFunction<?> stringifier) {
        StringBuilder sb = new StringBuilder(size() * 8);
        for (int i = 0; i < size(); i++) {
            sb.append(stringifier.apply(valueAt(i)));
            if (i != size() - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }
}
