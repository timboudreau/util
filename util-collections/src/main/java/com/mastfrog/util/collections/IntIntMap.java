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

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;

/**
 * An integer-keyed, integer-valued map with a small footprint and high
 * performance.
 *
 * @author Tim Boudreau
 */
public interface IntIntMap extends Map<Integer, Integer>, IntegerKeyedMap {

    /**
     * Create a new IntIntMap.
     *
     * @param initialCapacity The capacity to preallocate space for
     * @return A map
     */
    static IntIntMap create(int initialCapacity) {
        return new IntIntMapImpl(initialCapacity);
    }

    /**
     * Create a new IntIntMap.
     *
     * @return A map
     */
    static IntIntMap create() {
        return new IntIntMapImpl();
    }

    /**
     * Create a new IntIntMap from an array of keys and an array of values.
     *
     * @param keys The keys array
     * @param values The values array (must be the same length as the keys
     * array)
     * @param preSorted If true, the resulting map will assume the keys are
     * already sorted low-to-high. <i>If they are not, very bad things may
     * happen</i>.
     * @param checkDuplicates Pass true unless you are <i>absolutely sure</i>
     * there cannot be duplicates in the keys array you are passing.
     * @throws IllegalArgumentException if the array lengths do not match, or if
     * the keys array contains duplicates
     * @return A map
     */
    static IntIntMap of(int[] keys, int[] values, boolean preSorted, boolean checkDuplicates) {
        return new IntIntMapImpl(keys, values, checkDuplicates, preSorted);
    }

    /**
     * Create a new IntIntMap from an array of keys and array of values with
     * <i>no length or sorting or sanity checks</i> &emdash; use only if you can
     * <b>guarantee</b> that the arrays are the same length and the keys array
     * is already sorted; if you issue a put to the resulting map, it may
     * <i>modify the original arrays</i>. Use with caution when you can
     * guarantee that the invariants (pre-sorted keys array, no duplicate keys,
     * keys and values arrays the same length) will hold. Note that if they
     * don't, in particular in the case of duplicate or unsorted keys, it is
     * possible for a call to <code>get()</code> to enter an endless loop.
     *
     * @param keys The keys array
     * @param values The values array
     * @return an IntIntMap
     */
    static IntIntMap createUnsafe(int[] keys, int[] values) {
        return new IntIntMapImpl(keys, values);
    }

    /**
     * Put a key/value pair into the map.
     *
     * @param key A key
     * @param val A value
     */
    void put(int key, int val);

    /**
     * Get the value corresponding to a key.
     *
     * @param key A key
     * @return The value corrsponding to it
     * @throws NoSuchElementException if the value is not present
     */
    int getAsInt(int key);

    /**
     * Get the value corresponding to a key, or a default value if not present
     *
     * @param key The key
     * @param defaultValue The default value to return if not present
     * @return A value
     */
    int getAsInt(int key, int defaultValue);

    /**
     * Remove the key/value pair corresponding to the passed key.
     *
     * @param key The key
     * @return True if the collection was modified
     */
    boolean remove(int key);

    /**
     * Remove all of the passed keys.
     *
     * @param keys The keys
     * @return The number of items removed
     */
    int removeAll(IntSet keys);

    /**
     * Iterate each value.
     *
     * @param c A consumer
     */
    void forEachValue(IntConsumer c);

    /**
     * Iterate key/value pairs.
     *
     * @param c A consumer
     */
    void forEachPair(IntIntMapConsumer c);

    /**
     * Get the value at the passed index in this map's iteration order.
     *
     * @param index An index
     * @return The value at that index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    int valueAt(int index);

    /**
     * Set the value at a given index.
     *
     * @param index An index between zero and size, exclusive
     * @param value The replacement value
     * @return the old value or -1
     */
    int setValueAt(int index, int value);

    /**
     * A consumer for key/value pairs.
     */
    interface IntIntMapConsumer {

        // XXX this should be IntBiConsumer but we don't depend on
        // mastfrog functions lib
        /**
         * Called to visit one key/value pair.
         *
         * @param key A key
         * @param value A value
         */
        void item(int key, int value);
    }
}
