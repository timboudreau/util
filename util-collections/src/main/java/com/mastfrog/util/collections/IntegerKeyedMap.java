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

import com.mastfrog.util.search.Bias;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

/**
 * Base interface for maps such as IntMap and IntIntMap which use sorted arrays
 * of integers as their keys.
 *
 * @author Tim Boudreau
 */
public interface IntegerKeyedMap {

    /**
     * Get the set of keys of this map as a read-only IntSet.
     *
     * @return An IntSet
     */
    IntSet keySet();

    /**
     * The index of the passed key, if present.
     *
     * @param key The key
     * @return The index
     */
    int indexOf(int key);

    /**
     * Remove the passed set of indices from this map (in as few bulk array
     * operations as possible).
     *
     * @param indices The indices
     * @return The number of indices removed
     * @throws IndexOutOfBoundsException if any of the indices are out of range
     */
    int removeIndices(IntSet indices);

    /**
     * Get a copy of this map's keys as an array.
     *
     * @return The keys array
     */
    int[] keysArray();

    /**
     * Get the key at the passed index in this map's iteration order (typically
     * sorted low-to-high).
     *
     * @param index The index, less than size and greater than zero
     * @return The key at that index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    int key(int index);

    /**
     * Get a primitive iterator over this map's keys.
     *
     * @return An iterator
     */
    PrimitiveIterator.OfInt keysIterator();

    /**
     * Get the key nearest in value to the passed key.
     *
     * @param key A key
     * @param backward If true, search for the nearest key
     * <i>less than</i> rather than <i>greater than</i> the passed key.
     * @return The key
     */
    int nearestKey(int key, boolean backward);

    /**
     * Determine if this map contains the passed key.
     *
     * @param key A key
     * @return True if it is present
     */
    boolean containsKey(int key);

    /**
     * Get the highest key currently present, or -1 if empty.
     *
     * @return The highest key
     */
    int greatestKey();

    /**
     * Get the lowest key currently present, or -1 if empty.
     *
     * @return The lowest key
     */
    int leastKey();

    /**
     * Get the size of the map.
     *
     * @return The size
     */
    int size();

    /**
     * Get the index of the key whose value is closest to the passed
     * key.
     *
     * @param key A key value
     * @param backward If true, find the nearest key <i>less than</i>
     * the passed value
     * @return An index or -1 if none can be found traversing the keys in
     * the direction specified
     */
    int nearestIndexTo(int key, boolean backward) ;

    /**
     * Visit each key with an <code>IntConsumer</code>.
     *
     * @param cons An int consumer
     */
    default void forEachKey(IntConsumer cons) {
        int[] k = keysArray();
        for (int i = 0; i < k.length; i++) {
            cons.accept(k[i]);
        }
    }

    /**
     * Get a key which is present in this map and is equal to the passed key
     * value, or of not present, the value which is nearestKey to the passed one
     * in the direction specified by the passed bias (NONE = exact, BACKWARD
     * returns the nearestKey key less than the passed value, FORWARD returns
     * the nearestKey key greater than the passed value, NEAREST searches
     * forward and backward and returns whichever value is less distant,
     * preferring the forward value when equidistant).
     *
     * @param key The key
     * @param bias The bias to use if an exact match is not present
     * @return An key value which is present in this map
     */
    default int nearestKey(int key, Bias bias) {
        switch (bias) {
            case NONE:
                return containsKey(key) ? key : -1;
            case BACKWARD:
                return IntegerKeyedMap.this.nearestKey(key, true);
            case FORWARD:
                return IntegerKeyedMap.this.nearestKey(key, false);
            case NEAREST:
                int back = IntegerKeyedMap.this.nearestKey(key, false);
                int fwd = IntegerKeyedMap.this.nearestKey(key, true);
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
}
