/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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

import com.mastfrog.util.search.Bias;
import java.io.Serializable;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * Primitive int to object map; the default implementation uses internal arrays
 * and binary search for fast lookup of infrequently modified arrays, with some
 * cost to out-of-order adds on the next call to get(). Originally written for
 * the NetBeans output window to manage relative newline positions, this class
 * offers considerable performance benefits over a
 * <code>Map&lt;Integer, T&gt.</code> when building indexes into data structures
 * that are append-only or processed sequentially, such that added keys are
 * always higher than existing ones.
 * <p>
 * <b>Note on negative keys:</b>Since search operations for keys use -1 to
 * indicate that no value is present, fuzzy search operations for nearest keys
 * should not be used if the map can contain negative numbered keys.
 * </p>
 *
 * @author Tim Boudreau
 */
public interface IntMap<T> extends Iterable<Map.Entry<Integer, T>>, Map<Integer, T>, Serializable {

    /**
     * Like Map.containsKey(), determine if a key is present.
     *
     * @param key The key
     * @return Whether it is present or not
     */
    boolean containsKey(int key);

    /**
     * Decrement keys in the map. Entries with negative keys will be removed.
     *
     * @param decrement Value the keys should be decremented by. Must be zero or
     * higher.
     */
    void decrementKeys(int decrement);

    /**
     * Get the value with the least key in this map, or null if empty.
     *
     * @return The least value
     */
    T leastValue();

    /**
     * Get the value with the greatest key in this map, or null if empty.
     *
     * @return The least value
     */
    T greatestValue();

    public static <T> IntMap<T> of(int[] keys, T[] vals) {
        return new ArrayIntMap<>(keys, vals);
    }

    /**
     * Create a read-only, singleton IntMap.
     *
     * @param <T> The value type
     * @param key The key
     * @param val The value
     * @return A read only single element map
     */
    public static <T> IntMap<T> singleton(int key, T val) {
        return new SingletonIntMap<>(key, val);
    }

    T valueAt(int index);

    /**
     * Get the keys as an array.
     *
     * @return The keys
     */
    default int[] keysArray() {
        int[] result = new int[size()];
        int ix = 0;
        for (OfInt oi = keysIterator(); oi.hasNext();) {
            result[ix++] = oi.nextInt();
        }
        return result;
    }

    /**
     * Get the values as an array.
     *
     * @return The values
     */
    default Object[] valuesArray() {
        Object[] result = new Object[size()];
        int ix = 0;
        for (OfInt oi = keysIterator(); oi.hasNext();) {
            result[ix++] = get(oi.nextInt());
        }
        return result;
    }

    /**
     *
     * @return
     */
    Iterable<Map.Entry<Integer, T>> entries();

    /**
     * Remove a key returning the item for it, if any.
     *
     * @param key A key
     * @return A value or null
     */
    default T remove(int key) {
        return remove(Integer.valueOf(key));
    }

    /**
     * Get the map entry corresponding to a key (or if not present and this map
     * was created with a Supplier&lt;T&gt;, the value it provides potentially
     * adding it to the map if that behavior was specified at
     * construction-time).
     *
     * @param key The key
     * @return An object or null
     */
    T get(int key);

    /**
     * Get the map entry corresponding to a key, using the passed default value
     * if not present (will not use any Supplier provided at construction-time).
     *
     * @param key The key
     * @param defaultValue The default value to use when not present
     * @return An object or null
     */
    T getIfPresent(int key, T defaultValue);

    /**
     * Get a copy of the keys array for this map.
     *
     * @return An array of keys
     */
    int[] getKeys();

    /**
     * Get the highest key currently present, or -1 if empty.
     *
     * @return The highest key
     */
    int highestKey();

    /**
     * Get an iterator over the keys.
     *
     * @return An iterator
     */
    PrimitiveIterator.OfInt keysIterator();

    /**
     * Get the lowest key currently present, or -1 if empty.
     *
     * @return
     */
    int lowestKey();

    /**
     * Get the key which is present in this map and is closest to the passed
     * value.
     *
     * @param key A key
     * @param backward If true, look for keys less than the passed value if it
     * is not present
     * @return An integer, -1 if not present
     */
    int nearest(int key, boolean backward);

    /**
     * Get a key which is present in this map and is equal to the passed key
     * value, or of not present, the value which is nearest to the passed one in
     * the direction specified by the passed bias (NONE = exact, BACKWARD
     * returns the nearest key less than the passed value, FORWARD returns the
     * nearest key greater than the passed value, NEAREST searches forward and
     * backward and returns whichever value is less distant, preferring the
     * forward value when equidistant).
     *
     * @param key The key
     * @param bias The bias to use if an exact match is not present
     * @return An key value which is present in this map
     */
    default int nearest(int key, Bias bias) {
        switch (bias) {
            case NONE:
                return containsKey(key) ? key : -1;
            case BACKWARD:
                return nearest(key, true);
            case FORWARD:
                return nearest(key, false);
            case NEAREST:
                int back = nearest(key, false);
                int fwd = nearest(key, true);
                int distBack = back < 0 ? Integer.MAX_VALUE : Math.abs(key - back);
                int distFwd = fwd < 0 ? Integer.MAX_VALUE : Math.abs(fwd - key);
                if (distFwd <= distBack) {
                    return fwd;
                } else {
                    return back;
                }
            default:
                throw new AssertionError(bias);
        }
    }

    /**
     * Get a value which is present in this map and is mapped to the passed key
     * value, or of not present, the key which is nearest to the passed one in
     * the direction specified by the passed bias (NONE = exact, BACKWARD
     * returns the nearest key less than the passed value, FORWARD returns the
     * nearest key greater than the passed value, NEAREST searches forward and
     * backward and returns whichever value is less distant, preferring the
     * forward value when equidistant).
     *
     * @param key The key
     * @param bias The bias to use if an exact match is not present
     * @return An object or null
     */
    default T nearestValue(int key, Bias bias) {
        int actualKey = nearest(key, bias);
        return actualKey == -1 ? null : get(key);
    }

    /**
     * Add an element to this map, or replace an existing one.
     *
     * @param key The key
     * @param val The value
     * @return The old value, if any
     */
    T put(int key, T val);

    /**
     * Array-like access - get the index of a particular key within this map's
     * internal data structures; needed for bulk operations where many items are
     * iterated and performing a search for each one is prohibitively expensive.
     *
     * @param key A key
     * @return An index or -1
     */
    int indexOf(int key);

    /**
     * Visit values between the two passed values (inclusive if present).
     *
     * @param first One value
     * @param second An another value
     * @param c A consumer
     * @return The number of visits performed
     */
    int valuesBetween(int first, int second, IntMapConsumer<T> c);

    /**
     * Visit all keys and values between the two passed values (inclusive if
     * present).
     *
     * @param first One value
     * @param second An another value
     * @param c A consumer
     * @return The number of visits performed
     */
    int keysAndValuesBetween(int first, int second, IntMapBiConsumer<T> c);

    /**
     * Set the value at a particular index.
     *
     * @param index An index
     * @param obj A value
     */
    void setValueAt(int index, T obj);

    /**
     * Remove the key/value pair at a given index.
     *
     * @param index The index
     * @return The item removed
     */
    T removeIndex(int index);

    int removeIf(Predicate<T> test);

    /**
     * Move the data at one index to another, optionally retaining or
     * modifying the old value.
     *
     * @param src The first key
     * @param dest The destination key
     * @param mover Performs the data modifications
     * @return The final new value
     */
    default T move(int src, int dest, EntryMover<T> mover) {
        if (src == dest) {
            return get(src);
        }
        int fromIx = indexOf(src);
        if (fromIx < 0) {
            throw new IllegalArgumentException("No item at " + src);
        }
        T oldValue = valueAt(fromIx);
        int toIx = indexOf(dest);
        if (toIx < 0) {
            return mover.onMove(src, oldValue, dest, null, (newOldValue, newNewValue) -> {
                if (newOldValue != null) {
                    setValueAt(fromIx, newOldValue);
                }
                if (newNewValue != null) {
                    put(dest, newNewValue);
                }
                if (newOldValue == null) {
                    removeIndex(toIx);
                }
            });
        } else {
            T newValue = valueAt(toIx);
            return mover.onMove(src, oldValue, dest, newValue, (newOldValue, newNewValue) -> {
                if (newOldValue != null) {
                    setValueAt(fromIx, newOldValue);
                }
                if (newNewValue != null) {
                    setValueAt(toIx, newNewValue);
                }
                if (newOldValue == null) {
                    removeIndex(fromIx);
                }
            });
        }
    }

    interface EntryMover<T> {

        T onMove(int oldKey, T oldValue, int newKey, T newValue, BiConsumer<T, T> oldNewReceiver);
    }

    @Override
    IntSet keySet();

    /**
     * Create a synchronized view of this map.
     *
     * @return A synchronized map
     */
    default IntMap<T> toSynchronizedIntMap() {
        return new IntMapSynchronized<>(this);
    }

    /**
     * Consumer for map elements with no unboxing penalty.
     *
     * @param <T> The type
     */
    @FunctionalInterface
    interface IntMapConsumer<T> {

        void accept(int key, T value);
    }

    /**
     * A consumer for map elements with indices.
     *
     * @param <T> The type
     */
    interface IntMapBiConsumer<T> {

        void accept(int index, int key, T value);
    }

    /**
     * Consumer for map elements with no unboxing penalty, which can abort
     * iteration by returning false.
     *
     * @param <T> The type
     */
    @FunctionalInterface
    interface IntMapAbortableConsumer<T> {

        /**
         * Visit a key/value pair
         *
         * @param key The key
         * @param value The value
         * @return true to continue iterating, false to stop
         */
        boolean accept(int key, T value);
    }

    /**
     * Iterate the keys.
     *
     * @param cons A consumer
     */
    default void forEachKey(IntConsumer cons) {
        int[] k = getKeys();
        for (int i = 0; i < k.length; i++) {
            cons.accept(k[i]);
        }
    }

    /**
     * Iterate key value pairs.
     *
     * @param cons A consumer
     */
    default void forEach(IntMapConsumer<? super T> cons) {
        int[] k = getKeys();
        for (int i = 0; i < k.length; i++) {
            T t = get(k[i]);
            cons.accept(k[i], t);
        }
    }

    /**
     * Iterate key value pairs with indices.
     *
     * @param cons A consumer
     */
    void forEachIndexed(IntMapBiConsumer<? super T> c);

    void forEachReversed(IntMapBiConsumer<? super T> c);

    void removeIndices(IntSet toRemove);

    int key(int index);

    /**
     * Iterate key value pairs, stopping if the passed consumer returns false.
     *
     * @param cons A consumer
     * @return true if consumption was aborted by the consumer, false if
     * iteration was completed.
     */
    default boolean forSomeKeys(IntMapAbortableConsumer<? super T> cons) {
        int[] k = getKeys();
        for (int i = 0; i < k.length; i++) {
            T t = get(k[i]);
            boolean result = cons.accept(k[i], t);
            if (!result) {
                return false;
            }
        }
        return true;
    }
}
