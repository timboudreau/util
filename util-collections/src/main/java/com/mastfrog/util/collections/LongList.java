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

import static com.mastfrog.util.preconditions.Checks.greaterThanZero;
import static com.mastfrog.util.preconditions.Checks.notNull;
import com.mastfrog.util.search.Bias;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.LongConsumer;

/**
 * Extension to java.util.List which stores primitive longs and provides
 * accessors that do not require autoboxing for performance. Implementations
 * support binary search if the underlying data is sorted.
 *
 * @author Tim Boudreau
 */
public interface LongList extends List<Long> {

    /**
     * Create a new LongList from a copy of the passed array of values.
     *
     * @param values
     * @return A list
     */
    public static LongList of(long... values) {
        return new LongListSimple(notNull("values", values));
    }

    /**
     * Create a new LongList, <i>using the passed array as its internal
     * storage</i> (at least until elements are added and the array needs to be
     * grown); set operations on this collection will modify the passed array
     * and vice-versa. Use for arrays you expect to be immutable.
     *
     * @param values An array of values
     * @return A list
     */
    public static LongList unsafe(long[] values) {
        return new LongListSimple(notNull("values", values), true);
    }

    /**
     * Create a long list.
     *
     * @param partitioned If true, use the implementation which partitions its
     * contents into multiple arrays, allowing for cheap inserts at the
     * boundaries and reuse of array instances
     * @param values An array of values
     * @return A long list
     */
    public static LongList of(boolean partitioned, long... values) {
        return partitioned ? new LongListImpl(values) : new LongListSimple(values);
    }

    /**
     * Create a new empty long list.
     *
     * @param partitioned If true, use the implementation which partitions its
     * contents into multiple arrays, allowing for cheap inserts at the
     * boundaries and reuse of array instances
     * @param initialCapacity The initial storage size to preallocate
     * @return A long list
     */
    public static LongList create(boolean partitioned, int initialCapacity) {
        return partitioned ? new LongListImpl(greaterThanZero("initialCapacity", initialCapacity))
                : new LongListSimple(greaterThanZero("initialCapacity", initialCapacity));
    }

    /**
     * Create a new empty long list with preallocated storage of the passed
     * size.
     *
     * @param initialCapacity The initial storage size to preallocate
     * @return A long list
     */
    public static LongList create(int initialCapacity) {
        return new LongListSimple(greaterThanZero("initialCapacity", initialCapacity));
    }

    /**
     * Create a new empty LongList which can pool and reuse arrays, for
     * repeatedly processing large amounts of data which requires extensive
     * lists of longs.
     *
     * @param batchSize The size of individual arrays to allocate
     * @param initialArrayPoolSize The initial size of the array pool - zero for
     * no pooling (you just want the partitioning behavior)
     * @param maxArrayPoolSize The maximum array pool size
     * @return A list
     */
    public static LongList create(int batchSize, int initialArrayPoolSize, int maxArrayPoolSize) {
        return new LongListImpl(greaterThanZero("batchSize", batchSize),
                initialArrayPoolSize, // can be zero
                greaterThanZero("maxArrayPoolSize", maxArrayPoolSize));
    }

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
    boolean addAll(long... longs);

    /**
     * Insert an array of longs at the specified index.
     *
     * @param index The index for the insert - must be less than or equal to
     * size().
     * @param longs The values, must be non-null
     * @return True if the array is non-empty
     */
    boolean addAll(int index, long[] longs);

    /**
     * Test if the passed long is present in this list.
     *
     * @param test The long to search for
     * @return True if it is present
     */
    default boolean contains(long test) {
        for (int i = 0; i < size(); i++) {
            if (getAsLong(i) == test) {
                return true;
            }
        }
        return false;
    }

    /**
     * Iterate using a LongConsumer.
     *
     * @param consumer A LongConsumer
     */
    void forEach(LongConsumer consumer);

    /**
     * Reverse-iterate using a LongConsumer.
     *
     * @param c A LongConsumer
     */
    default void forEachReversed(LongConsumer c) {
        int ix = size() - 1;
        while (ix > 0) {
            c.accept(getAsLong(ix--));
        }
    }

    /**
     * Get the last element.
     *
     * @return The last element
     * @throws IndexOutOfBoundsException if empty
     */
    default long last() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException();
        }
        return getAsLong(size() - 1);
    }

    /**
     * Get the first element
     *
     * @return The first element
     * @throws IndexOutOfBoundsException if empty
     */
    default long first() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException();
        }
        return getAsLong(0);
    }

    /**
     * Get a the primitive long at the specified index.
     *
     * @param index The offset
     * @return The value at that offset
     */
    long getAsLong(int index);

    /**
     * Find the first index of a value.
     *
     * @param test The value to look for
     * @return The offset, or -1 if not present
     */
    int indexOf(long test);

    /**
     * Determine whether this list is <i>definitely</i> sorted - as in, does not
     * contain duplicates and was sorted by the sort method or all elements were
     * sequential when added.
     *
     * @return True if the list is definitely sorted; if false, it might be, but
     * also might not be
     */
    boolean isSorted();

    /**
     * Remove a value, if present, returning its former offset
     *
     * @param val The value
     * @return The offset or -1
     */
    int removeValue(long val);

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
    @Override
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
    LongList copy();

    /**
     * Remove the element at the passed index between 0 and size exclusive.
     *
     * @param index An index
     * @return The value formerly at that index
     */
    long removeAt(int index);

    /**
     * Remove the last element of this collection, reducing its size by one.
     *
     * @return true if there was an element to remove
     */
    boolean removeLast();

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
    @SuppressWarnings("deprecation")
    Longerator longerator();

    /**
     * Sort this list, returning true if the result is fully sorted (no
     * duplicates) and usable for binary search.
     *
     * @return True if the list is sorted and contains no duplicates
     */
    boolean sort();

    /**
     * Overridden to return PrimitiveIterator.OfLong.
     *
     * @return An iterator
     */
    @Override
    PrimitiveIterator.OfLong iterator();

    /**
     * Determine if this list <i>starts with</i> but is <i>not the same as</i>
     * this list; returns false for the empty list, itself and an equal list,
     * and true for lists whose length is less than this one and whose contents
     * up to the end of the passed list are the same.
     *
     * @param others
     * @return True if this list starts with the values in the passed list
     * @since 2.6.13.4
     */
    default boolean startsWith(List<Long> others) {
        if (notNull("others", others).size() >= size() || isEmpty() || others.isEmpty()) {
            return false;
        }
        if (others instanceof LongList) {
            for (int i = 0; i < ((LongList) others).size(); i++) {
                if (getAsLong(i) != ((LongList) others).getAsLong(i)) {
                    return false;
                }
            }
            return true;
        }
        for (int i = 0; i < others.size(); i++) {
            if (getAsLong(i) != others.get(i).longValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine if this list <i>ends with</i> but is <i>not the same as</i>
     * this list; returns false for the empty list, itself and an equal list,
     * and true for lists whose length is less than this one and whose contents
     * up to the end of the passed list are the same.
     *
     * @param others
     * @return True if this list ends with the values in the passed list
     * @since 2.6.13.4
     */
    default boolean endsWith(List<Long> others) {
        if (others.size() >= size() || others.isEmpty() || isEmpty()) {
            return false;
        }
        if (others instanceof LongList) {
            LongList ll = (LongList) others;
            for (int i = size() - 1, j = others.size() - 1; i > 0 && j >= 0; j--, i--) {
                if (getAsLong(i) != ll.getAsLong(j)) {
                    return false;
                }
            }
            return true;
        }
        for (int i = size() - 1, j = others.size() - 1; i > 0 && j >= 0; j--, i--) {
            if (getAsLong(i) != others.get(j).longValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the index of a value in this collection, <i>presuming it is
     * sorted</i> - the caller must <b><u>know, <i>for sure</i></u></b>
     * that this collection really is sorted before calling this method; if it
     * is not, it may loop endlessly or any of the other bad behaviors binary
     * searches are prone to when trying to operate on unsorted data.
     *
     * @param value The value to look for
     * @return The index of that value
     */
    int indexOfPresumingSorted(long value);

    /**
     * Get the index in this collection of the value nearest to the passed one,
     * using the passed bias to control what is considered a valid result for
     * purposes of fuzzy matching. This method <i>presumes it collection is
     * already sorted</i> - the caller must <b><u>know, <i>for sure</i></u></b>
     * that this collection really is sorted before calling this method; if it
     * is not, it may loop endlessly or any of the other bad behaviors binary
     * searches are prone to when trying to operate on unsorted data.
     *
     * @param value The value to look for
     * @return The index of that value
     */
    int nearestIndexToPresumingSorted(long value, Bias bias);

    /**
     * Swap the values at two indices in the list.
     *
     * @param ix1 The first index
     * @param ix2 The second index
     * @throws IllegalArgumentException if the size is &lt; 2, or if either
     * index is &lt;0 or &gt;= size().
     * @return True if the indices are not the same
     */
    default boolean swapIndices(int ix1, int ix2) {
        if (ix1 == ix2) {
            return false;
        }
        int sz = size();
        if (sz < 2) {
            throw new IllegalArgumentException("Cannot swap on an "
                    + "empty or single item list, and have " + sz + " items");
        }
        if (ix1 < 0) {
            throw new IllegalArgumentException("Negative first index " + ix1);
        }
        if (ix2 < 0) {
            throw new IllegalArgumentException("Negative second index " + ix2);
        }
        if (ix1 >= sz) {
            throw new IllegalArgumentException("First index >= size " + ix1 + " vs " + sz);
        }
        if (ix2 >= sz) {
            throw new IllegalArgumentException("Second index >= size " + ix1 + " vs " + sz);
        }
        long at1 = get(ix1);
        long at2 = get(ix2);
        if (at1 == at2) {
            return false;
        }
        set(ix1, at2);
        set(ix2, at1);
        return true;
    }

    /**
     * Swap the position of two values in this list. If either value is present
     * multiple times, it is not specified which instance will be swapped.
     *
     * @param v1 The first value
     * @param v2 The second value
     * @return true if both values are present, they are not the same value, and
     * the list was altered as a result of this call.
     */
    default boolean swap(long v1, long v2) {
        int ix1 = indexOf(v1);
        int ix2 = indexOf(v2);
        if (ix1 == ix2) {
            return false;
        }
        if (ix1 < 0 || ix2 < 0) {
            return false;
        }
        long at1 = get(ix1);
        long at2 = get(ix2);
        set(ix1, at2);
        set(ix2, at1);
        return true;
    }

    @Override
    default Spliterator.OfLong spliterator() {
        return new ArrayLongSpliterator(toLongArray());
    }
}
