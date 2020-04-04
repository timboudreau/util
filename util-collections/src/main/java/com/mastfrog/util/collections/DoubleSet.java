/*
 * The MIT License
 *
 * Copyright 2020 Tim Boudreau.
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

import com.mastfrog.util.preconditions.Checks;
import com.mastfrog.util.search.Bias;
import java.util.Collection;
import java.util.PrimitiveIterator;
import java.util.function.DoubleConsumer;

/**
 * A set of primitive doubles, backed by an array, optimized for performance,
 * with fuzzy lookup methods. Intentionally does not implement
 * <code>java.util.Set</code>, because things like returning true of false from
 * add and remove methods would make certain lazy computations impossible.
 *
 * @author Tim Boudreau
 */
public interface DoubleSet extends Iterable<Double>, Trimmable {

    public static DoubleSet of(Collection<? extends Number> c) {
        Checks.notNull("c", c);
        return DoubleSetImpl.of(c);
    }

    public static DoubleSet ofFloats(float... floats) {
        Checks.notNull("floats", floats);
        return DoubleSetImpl.ofFloats(floats);
    }

    public static DoubleSet ofDoubles(double... doubles) {
        Checks.notNull("doubles", doubles);
        return DoubleSetImpl.ofDoubles(doubles.length, doubles);
    }

    public static DoubleSet ofDoubles(int capacity, double... doubles) {
        Checks.notNull("doubles", doubles);
        Checks.nonNegative("capacity", capacity);
        return DoubleSetImpl.ofDoubles(capacity, doubles);
    }

    public static DoubleSet ofInts(int... ints) {
        return ofInts(ints.length, ints);
    }

    public static DoubleSet ofInts(int capacity, int[] ints) {
        Checks.notNull("ints", ints);
        Checks.nonNegative("capacity", capacity);
        DoubleSetImpl impl = new DoubleSetImpl(capacity);
        for (int i = 0; i < ints.length; i++) {
            impl.add(ints[i]);
        }
        return impl;
    }

    public static DoubleSet create() {
        return new DoubleSetImpl();
    }

    public static DoubleSet create(int capacity) {
        Checks.nonNegative("capacity", capacity);
        return new DoubleSetImpl(capacity);
    }

    public static DoubleSet emptyDoubleSet() {
        return DoubleSetEmpty.INSTANCE;
    }

    /**
     * Create an independent copy of this double set with the same contents.
     *
     * @return Another DoubleSet
     */
    DoubleSet copy();

    /**
     * Add a value to this set; if already present, the size will be unchanged.
     * Implementation note: The value Double.MIN_VALUE is illegal to add, since
     * it is used as a null return value and would make such return values
     * ambiguous. For the set of use cases of this class (primarily fast lookup
     * of geometric points), this is a non-issue.
     *
     * @param value The value
     */
    void add(double value);

    /**
     * Add the contents of another DoubleSet to this one.
     *
     * @param set Another set
     */
    void addAll(DoubleSet set);

    /**
     * Add an array of doubles to this DoubleSet.
     *
     * @param doubles
     */
    default void addAll(double[] doubles) {
        addAll(DoubleSetImpl.ofDoubles(doubles));
    }

    /**
     * Add an array of floats to this DoubleSet.
     *
     * @param floats An array of floats
     */
    default void addAll(float[] floats) {
        for (int i = 0; i < floats.length; i++) {
            add(floats[i]);
        }
    }

    default boolean containsApproximate(double targetValue, double tolerance) {
        if (targetValue == Double.MIN_VALUE) {
            throw new IllegalArgumentException("Double.MIN_VALUE is the "
                    + "null return value for nearest values and cannot"
                    + "be used here.");
        }
        double result = nearestValueTo(targetValue, tolerance);
        return result != Double.MIN_VALUE;
    }

    /**
     * Clear the contents of this set, setting its size to zero; this operation
     * may not release underlying storage.
     *
     */
    void clear();

    /**
     * Determine if this set contains a value.
     *
     * @param d A value
     * @return True if the value is present
     */
    boolean contains(double d);

    /**
     * Visit all doubles in this set in ascending order.
     *
     * @param dc A consumer
     */
    void forEachDouble(DoubleConsumer dc);

    /**
     * Visit all doubles in this set in descending order.
     *
     * @param dc A consumer
     */
    void forEachReversed(DoubleConsumer dc);

    /**
     * Get the value at the specified index. Since DoubleSets are array-backed,
     * items can be addressed by index as well as value.
     *
     * @param index The index
     * @return A value
     * @throws IndexOutOfBoundsException if the index is &lt; 0 or &gt;=size
     */
    double getAsDouble(int index);

    /**
     * Get the greatest value in this set, or Double.MIN_VALUE if the set is
     * empty.
     *
     * @return The greatest value in the set
     */
    double greatest();

    /**
     * Get the index of the passed value, if exactly present.
     *
     * @param d A value
     * @return An index, or -1 if not present
     */
    int indexOf(double d);

    /**
     * Determine if this set is empty.
     *
     * @return True if it is empty
     */
    boolean isEmpty();

    /**
     * Get an iterator.
     *
     * @return An iterator
     */
    @Override
    PrimitiveIterator.OfDouble iterator();

    /**
     * Get the least value present, or Double.MIN_VALUE if not present.
     *
     * @return The least value present
     */
    double least();

    /**
     * Get the index of the closest value to the passed one, using the passed
     * bias to determine how to resolve inexact matches.
     *
     * @param approximateValue The (approximate in the case of most biases)
     * value to search for
     * @param bias The bias that determines how to search for a return value in
     * the case that an exact match is not present
     * @return The nearest index, or -1 if not present
     */
    int nearestIndexTo(double approximateValue, Bias bias);

    /**
     * Get the value closest to the specified value where the distance, positive
     * or negative, to that value is less than or equal to the passed tolerance;
     * returns Double.MIN_VALUE as null result.
     *
     * @param approximateValue A value
     * @return The nearest value to that value
     */
    double nearestValueTo(double approximateValue, double tolerance);

    /**
     * Like <code>nearestValueTo(double approximateValue)</code>, but if the
     * returned value would be an exact match to the passed value, use the next
     * closest value.
     *
     * @param approximateValue The value to look for
     * @return The nearest value or Double.MIN_VALUE if none
     */
    double nearestValueExclusive(double approximateValue);

    /**
     * Like
     * <code>nearestValueTo(double approximateValue, double tolerance)</code>,
     * but if the returned value would be an exact match to the passed value,
     * use the next closest value.
     *
     * @param approximateValue The value to look for
     * @param tolerance The maximum difference between the approximate value and
     * the returned value before Double.MIN_VALUE should be returned to indicate
     * no result.
     * @return The nearest value or Double.MIN_VALUE if none
     */
    double nearestValueExclusive(double approximateValue, double tolerance);

    /**
     * Get the value closest to the specified value; returns Double.MIN_VALUE as
     * null result.
     *
     * @param approximateValue A value
     * @return The nearest value to that value
     */
    double nearestValueTo(double approximateValue);

    /**
     * Partition this set into several smaller sets, leaving the original
     * unaltered.
     *
     * @param maxPartitions
     * @return
     */
    DoubleSet[] partition(int maxPartitions);

    /**
     * Get the value of greatest() - least().
     *
     * @return The range
     */
    double range();

    /**
     * Remove all items in the passed array.
     *
     * @param doubles An array of values
     */
    default void removeAll(double... doubles) {
        removeAll(DoubleSetImpl.ofDoubles(doubles));
    }

    /**
     * Remove all elements which are present in this set and the passed one.
     *
     * @param remove A set of items to remove
     */
    void removeAll(DoubleSet remove);

    /**
     * Remove all elements from this set which are not also in the passed one.
     *
     * @param retain The items to retain
     */
    void retainAll(DoubleSet retain);

    /**
     * Get the number of elements in this set
     *
     * @return The size
     */
    int size();

    /**
     * Get a sorted, duplicate-free array of the contents of this set.
     *
     * @return
     */
    double[] toDoubleArray();

    /**
     * Create a read-only view of this set, which shares data with it (and, like
     * Collections.unmodifiable*(), will change if the underlying data changes).
     *
     * @return A set whose mutation methods throw an
     * UnsupportedOperationException
     */
    DoubleSet unmodifiableView();

    /**
     * Create a read-only copy of this set which is immutable and unaffected by
     * changes to this set's value.
     *
     * @return A read only copy
     */
    DoubleSet toReadOnlyCopy();

    /**
     * Remove all elements between the passed least and greatest values,
     * inclusive.
     *
     * @param least The first value
     * @param greatest The second value
     * @return the number of items removed
     */
    int removeRange(double least, double greatest);

    boolean remove(double key);

    default DoubleSet subset(double least, double greatest) {
        int first = nearestIndexTo(Math.min(least, greatest), Bias.FORWARD);
        int last = nearestIndexTo(Math.max(least, greatest), Bias.BACKWARD);
        DoubleSetImpl result = new DoubleSetImpl((last - first) + 1);
        for (int i = first; i < last; i++) {
            result.add(getAsDouble(i));
        }
        return result;
    }

    default DoubleSet toSynchronizedSet() {
        return new DoubleSetImpl.SynchronizedDoubleSet(this);
    }
}
