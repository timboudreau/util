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

import java.util.List;
import java.util.Objects;

/**
 * An array-backed map of primitive doubles to values of some type. Note that,
 * unlike a HashMap, the default implementation is optimized for minimizing
 * re-sorts with bulk operations, so the behavior of duplicate puts is that one
 * of either the existing or new values will be retained.
 *
 * @author Tim Boudreau
 */
public interface DoubleMap<T> extends Trimmable {

    /**
     * Create a new map with the default capacity of 128.
     *
     * @param <T> The type
     * @return A map
     */
    static <T> DoubleMap<T> create() {
        return create(128);
    }

    /**
     * Create a new map with the default capacity of 128.
     *
     * @param <T> The type
     * @param initialCapacity The initial array size to allocate
     * @return A map
     */
    static <T> DoubleMap<T> create(int initialCapacity) {
        return new DoubleMapImpl<>(initialCapacity);
    }

    static <T> DoubleMap<T> emptyDoubleMap() {
        return DoubleMapEmpty.coerce();
    }

    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Remove an item by index.
     *
     * @param index The index, &gt;= 0 and &lt; size
     */
    void removeIndex(int index);

    /**
     * Remove multiple items by index.
     *
     * @param indices A set of indices
     */
    void removeIndices(IntSet indices);

    /**
     * Add a key/value pair.
     *
     * @param key The key
     * @param value The value
     */
    void put(double key, T value);

    /**
     * Get the corresponding value for a key, if any.
     *
     * @param key The key
     * @return The value or null
     */
    T get(double key);

    /**
     * Get the corresponding value for a key, returning the passed default value
     * if it is not present.
     *
     * @param key The key
     * @param defaultResult The failover result
     * @return The result
     */
    T getOrDefault(double key, T defaultResult);

    /**
     * Get the number of elements in this map.
     *
     * @return The size of the map
     */
    int size();

    /**
     * Determine if the exact key passed is present.
     *
     * @param d The key
     * @return true if it is present
     */
    boolean containsKey(double d);

    /**
     * Get the (read-only) key set for this map.
     *
     * @return A set of doubles
     */
    DoubleSet keySet();

    /**
     * Get the key for one element in this map.
     *
     * @param index The index
     * @return The key at that index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    double key(int index);

    /**
     * Get the value of one element in this map.
     *
     * @param index The index
     * @return The value
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    T valueAt(int index);

    /**
     * Replace the value at a given index. While put() of duplicate keys may or
     * may not replace the value, this always will.
     *
     * @param index The index The internal index of the value
     * @param value The new value The new value
     * @return The old value
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    T setValueAt(int index, T value);

    /**
     * Get all values in this map as a list.
     *
     * @return The list of values
     */
    List<T> values();

    /**
     * Get the index at which the passed exact key value appears.
     *
     * @param key The key
     * @return The index or -1
     */
    int indexOf(double key);

    /**
     * Put the entire contents of the passed map. In the case of overlaps, it is
     * undefined which value is retained.
     *
     * @param map Another map
     */
    void putAll(DoubleMap<T> map);

    /**
     * Given an approximate key value, visit the nearest key/value pair where
     * the actual map key's value is less than <code>approximate</code> +/-
     * <code>tolerance</code>, and where <i>the key value is <u>not exactly
     * equal to the passed approximate key</u></i> if any.
     *
     * @param approximate An approximate key value
     * @param tolerance The distance from the approximate key that an actual key
     * can be permitted to have and still match the query
     * @param c A visitor that will receive the index, key and value.
     * @return True if a match was found and the visitor was called
     */
    boolean nearestValueExclusive(double approximate, double tolerance, DoubleMapConsumer<? super T> c);

    default Entry<? extends T> nearestValueExclusive(double approximate, double tolerance) {
        Entry<T> result = new Entry<>();
        if (nearestValueExclusive(approximate, tolerance, result::setParams)) {
            return result;
        }
        return null;
    }

    /**
     * Given an approximate key value, visit the nearest key/value pair to the
     * passed approximate key <i>where that key is <u>not exactly equal to the
     * passed key</u></i>.
     *
     * @param approximate An approximate key value
     * @param tolerance The distance from the approximate key that an actual key
     * can be permitted to have and still match the query
     * @param c A visitor that will receive the index, key and value.
     * @return True if a match was found and the visitor was called
     */
    boolean nearestValueExclusive(double approximate, DoubleMapConsumer<? super T> c);

    default Entry<? extends T> nearestValueExclusive(double approximate) {
        Entry<T> result = new Entry<>();
        if (nearestValueExclusive(approximate, result::setParams)) {
            return result;
        }
        return null;
    }

    /**
     * Given an approximate key value, visit the nearest key/value pair to the
     * passed approximate key.
     *
     * @param approximate An approximate key value
     * @param tolerance The distance from the approximate key that an actual key
     * can be permitted to have and still match the query
     * @param c A visitor that will receive the index, key and value.
     * @return True if a match was found and the visitor was called
     */
    boolean nearestValueTo(double approximate, DoubleMapConsumer<? super T> c);

    default Entry<? extends T> nearestValueTo(double approximate) {
        Entry<T> result = new Entry<>();
        if (nearestValueTo(approximate, result::setParams)) {
            return result;
        }
        return null;
    }

    /**
     * Given an approximate key value, visit the nearest key/value pair where
     * the actual map key's value is less than <code>approximate</code> +/-
     * <code>tolerance</code>, if any.
     *
     * @param approximate An approximate key value
     * @param tolerance The distance from the approximate key that an actual key
     * can be permitted to have and still match the query
     * @param c A visitor that will receive the index, key and value.
     * @return True if a match was found and the visitor was called
     */
    boolean nearestValueTo(double approximate, double tolerance, DoubleMapConsumer<? super T> c);

    default Entry<? extends T> nearestValueTo(double approximate, double tolerance) {
        Entry<T> result = new Entry<>();
        if (nearestValueTo(approximate, tolerance, result::setParams)) {
            return result;
        }
        return null;
    }

    /**
     * Visit all key/value pairs whose key is greater than or equal to the
     * greater of the two passed doubles, and less than or equal to the the
     * lesser of the two passed doubles.
     *
     * @param firstOrLast1 A key value
     * @param firstOrLast2 Another key value
     * @param c A consumer
     * @return The number of items visited
     */
    int valuesBetween(double firstOrLast1, double firstOrLast2,
            DoubleMapConsumer<? super T> c);

    /**
     * Visit the key/value pair with the greatest key value.
     *
     * @param c A visitor
     * @return True if the visitor was called (map is non-empty)
     */
    boolean greatest(DoubleMapConsumer<? super T> c);

    default Entry<? extends T> greatest() {
        Entry<T> result = new Entry<>();
        if (greatest(result::setParams)) {
            return result;
        }
        return null;
    }

    /**
     * Visit the key/value pair with the least key value.
     *
     * @param c A visitor
     * @return True if the visitor was called (map is non-empty)
     */
    boolean least(DoubleMapConsumer<? super T> c);

    default Entry<? extends T> least() {
        Entry<T> result = new Entry<T>();
        if (least(result::setParams)) {
            return result;
        }
        return null;
    }

    /**
     * Visit each key/value pair in succession.
     *
     * @param c A visitor
     */
    void forEach(DoubleMapConsumer<? super T> c);

    int removeRange(double start, double end);

    boolean remove(double key);

    void removeAll(double... keys);

    void removeAll(DoubleSet set);

    void clear();

    default Entry<T> getEntry(int index) {
        double key = key(index);
        T val = valueAt(index);
        return new Entry<>(index, key, val);
    }

    public static final class Entry<T> {

        private double key;
        private T value;
        private int index;

        Entry() {

        }

        Entry(int index, double key, T value) {
            this.key = key;
            this.index = index;
            this.value = value;
        }

        @Override
        public String toString() {
            return index + ". " + key + ":" + value;
        }

        void setParams(int index, double key, T value) {
            this.key = key;
            this.index = index;
            this.value = value;
        }

        public int index() {
            return index;
        }

        public double key() {
            return key;
        }

        public T value() {
            return value;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + (int) (Double.doubleToLongBits(this.key) ^ (Double.doubleToLongBits(this.key) >>> 32));
            hash = 53 * hash + Objects.hashCode(this.value);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Entry<?> other = (Entry<?>) obj;
            if (Double.doubleToLongBits(this.key) != Double.doubleToLongBits(other.key)) {
                return false;
            }
            if (!Objects.equals(this.value, other.value)) {
                return false;
            }
            return true;
        }

    }
}
