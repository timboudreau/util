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

import java.util.List;
import java.util.function.LongConsumer;

/**
 * Extension to java.util.List which stores primitive longs and provides
 * accessors that do not require autoboxing for performance. Implementation
 * supports binary search if the underlying data is sorted.
 *
 * @author Tim Boudreau
 */
public interface LongList extends List<Long> {

    /**
     * Add a long.
     *
     * @param l The value
     * @return True if it was added
     */
    boolean add(long l);

    /**
     * Add a long at the specified offset, shifting that index and subsequent
     * ones by one.
     *
     * @param index
     * @param element
     */
    void add(int index, long element);

    /**
     * Append an array of longs.
     *
     * @param longs The values
     * @return True if the array is non-empty
     */
    boolean addAll(long[] longs);

    /**
     * Insert an array of longs at the specified index.
     *
     * @param index The index for the insert - must be less than or equal to
     * size().
     * @param longs The values, must be non-null
     * @return True if the array is non-empty
     */
    boolean addAll(int index, long[] c);

    /**
     * Test if the passed long is present in this list.
     *
     * @param test The long to search for
     * @return True if it is present
     */
    boolean contains(long test);

    /**
     * Iterate using a LongConsumer.
     *
     * @param consumer A LongConsumer
     */
    void forEach(LongConsumer consumer);

    /**
     * Get a the primitive long at the specified index.
     *
     * @param index The offset
     * @return The value at that offset
     */
    long getLong(int index);

    /**
     * Find the first index of a value.
     *
     * @param test The value to look for
     * @return The offset, or -1 if not present
     */
    int indexOf(long test);

    /**
     * Determine whether this list is sorted.
     *
     * @return
     */
    boolean isSorted();

    /**
     * Remove a value, if present, returning its former offset
     *
     * @param val The value
     * @return The offset or -1
     */
    int removeLong(long val);

    /**
     * Set the value at an index to the passed primitive long.
     *
     * @param index The offset
     * @param element The value
     * @return The previous value at that position
     */
    long set(int index, long element);

    /**
     * Overloads List.subList to guarantee the result is a LongList.
     *
     * @param fromIndex The start index
     * @param toIndex The end index
     * @return A sub list
     */
    LongList subList(int fromIndex, int toIndex);

    /**
     * Convert the entire contents of this list to a primitive long array.
     *
     * @return An array
     */
    long[] toLongArray();

    /**
     * Create a copy of this list. Changes to this list will not affect the
     * returned list, and vice versa.
     *
     * @return A list
     */
    LongList duplicate();

    /**
     * Get the last index of a value.
     *
     * @param val The value
     * @return The index or -1
     */
    int lastIndexOf(long val);

    /**
     * Get the index of a sub-array.
     *
     * @param longs A sub array
     * @return 0 if the array is empty, -1 if not present, otherwise the first
     * index of the passed array's pattern of values in this list
     */
    int indexOfArray(long[] longs);

    /**
     * Remove a range of offsets from this list.
     *
     * @param fromIndex The from index
     * @param toIndex The to index
     */
    void removeRange(int fromIndex, int toIndex);

    /**
     * Get an iterator for primitive longs.
     *
     * @return A longerator
     */
    Longerator longerator();

    /**
     * Sort this list, returning true if the result is fully sorted (no
     * duplicates) and usable for binary search.
     *
     * @return True if the list is sorted and contains no duplicates
     */
    boolean sort();
}
