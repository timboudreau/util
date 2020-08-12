/*
 * The MIT License
 *
 * Copyright 2016 Tim Boudreau.
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

import static com.mastfrog.util.preconditions.Checks.notNull;
import com.mastfrog.util.search.Bias;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.IntConsumer;

/**
 * A set of primitive integers, which implements <code>Set&lt;Integer&gt;</code>
 * for convenience. For performance, these can be considerably faster than
 * standard collections, since they take advantage of binary search and bulk
 * array moves or bitset operations. Both BitSet and array-based implementations
 * are provided, and both also provide index-based array-like lookup (entries
 * sorted from low to high). The BitSet implementation should be preferred when:
 * <ul>
 * <li>No negative values need to be stored</li>
 * <li>The set is likely to be out-of-order-write intensive</li>
 * <li>Intersection or other logical operations over the entire set are
 * needed</li>
 * </ul>
 * <p>
 * </p>
 *
 * @author Tim Boudreau
 */
public abstract class IntSet implements Set<Integer>, Cloneable, Trimmable {

//    @HotSpotIntrinsicCandidate
    IntSet() {
    }

    /**
     * Create an IntSet backed by a BitSet (no negative values allowed, slower
     * array-style lookups).
     *
     * @param capacity The anticipated number of set members
     * @return An IntSet
     */
    public static IntSet bitSetBased(int capacity) {
        return new IntSetImpl(capacity);
    }

    /**
     * Create an IntSet backed by a BitSet (negative values allowed, faster
     * array-style lookups, slower discontiguous updates).
     *
     * @param capacity The anticipated number of set members
     * @return An integer set
     */
    public static IntSet arrayBased(int capacity) {
        return new IntSetArray(capacity);
    }

    /**
     * Create a BitSet backed IntSet pre-populated with the sequence of numbers
     * from zero up to and including <code>upTo</code>.
     *
     * @param upTo The last value to set, inclusive
     * @return An integer set
     */
    public static IntSet allOf(int upTo) {
        BitSet bits = new BitSet(upTo + 64);
        bits.set(0, upTo + 1);
        return new IntSetImpl(bits);
    }

    /**
     * Create an IntSet pre-populated with the sequence of numbers from
     * <code>from</code> up to and including <code>upTo</code>.
     *
     * @param upTo The last value to set, inclusive
     * @return An integer set, BitSet-backed unless <code>from &lt; 0</code>
     */
    public static IntSet allOf(int from, int upTo) {
        if (upTo < from) {
            int hold = from;
            from = upTo;
            upTo = hold;
        }
        if (from < 0) {
            IntSetArray result = new IntSetArray(Math.max(3, (upTo - from) + 1));
            for (int i = from; i <= upTo; i++) {
                result.add(i);
            }
            return result;
        }
        BitSet bits = new BitSet(upTo + 64);
        bits.set(from, upTo + 1);
        return new IntSetImpl(bits);
    }

    /**
     * Create an IntSet; assumes BitSet-backed in most cases.
     *
     * @param capacity The target capacity
     * @return
     */
    public static IntSet create(int capacity) {
        int targetBytes = capacity / Long.BYTES;
        if (targetBytes > 1073741824) {
            return new IntSetArray(capacity / 20);
        }
        return new IntSetImpl(capacity);
    }

    /**
     * Create an IntSet.
     *
     * @param minValue the minimum expected value (negative guarantees an
     * array-based return value)
     * @param maxValue The maximum expected values
     * @return
     */
    public static IntSet create(int minValue, int maxValue) {
        int len = Math.abs(maxValue - minValue) + 1;
        if (len > 1073741824) {
            return new IntSetArray(len / 20);
        }
        if (minValue < 0) {
            return new IntSetArray(len);
        }
        if (minValue > 32768) {
            return new IntSetArray(len / 4);
        }
        return new IntSetImpl(len);
    }

    public static IntSet create() {
        return create(96);
    }

    public static IntSet of(int... vals) {
        return create(vals);
    }

    /**
     * Create an IntSet from an array. The result will be either array- or
     * BitSet-backed depending on whether negative values are encountered, or
     * the lowest value present in the array would cause a substantial array of
     * empty longs to be allocated in a BitSet.
     *
     * @param arr An array
     * @return An integer set
     */
    public static IntSet create(int[] arr) {
        if (arr.length == 0) {
            return create();
        }
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < arr.length; i++) {
            min = Math.min(arr[i], min);
            if (arr[i] < 0) {
                break;
            }
        }
        if (min < 0 || min > Long.SIZE * 16) {
            return new IntSetArray(arr);
        }
        BitSet set = new BitSet(arr.length);
        for (int i = 0; i < arr.length; i++) {
            set.set(arr[i]);
        }
        return new IntSetImpl(set);
    }

    /**
     * Create a view of a BitSet as an IntSet - changes in either will be
     * reflected in the other.
     *
     * @param bits A BitSet
     * @return An IntSet
     */
    public static IntSet of(BitSet bits) {
        return new IntSetImpl(notNull("bits", bits));
    }

    /**
     * Create an IntSet whose initial contents are those of the passed BitSet;
     * changes in either will not alter the other.
     *
     * @param bits A bit set
     * @return An integer set
     */
    public static IntSet create(BitSet bits) {
        return new IntSetImpl((BitSet) notNull("bits", bits).clone());
    }

    /**
     * Create an IntSet from a standard Java Integer collection.
     *
     * @param set The collection
     * @return An integer set
     */
    public static IntSet create(Collection<? extends Integer> set) {
        return new IntSetImpl(set);
    }

    public static IntSet toIntSet(Collection<? extends Integer> set) {
        if (set instanceof IntSet) {
            return (IntSet) set;
        } else {
            return new IntSetImpl(set);
        }
    }

    /**
     * Create an array of booleans for this set, from 0 to the set's size. Note
     * that if this is an array-based set containing very large values, this
     * will allocate a <i>huge</i> array and is likely not what you want.
     *
     * @return An array of booleans
     */
    public boolean[] toBooleanArray(int size) {
        boolean[] result = new boolean[size];
        visitConsecutiveIndices((int first, int last, int count) -> {
            Arrays.fill(result, first, last + 1, true);
        });
        return result;
    }

    /**
     * Create an array of booleans for the values in this set between the start
     * and end values.
     *
     * @param start The start
     * @param end The end
     * @return An array of booleans
     */
    public boolean[] toBooleanArray(int start, int end) {
        boolean[] result = new boolean[end - start];
        for (int i = start; i < end; i++) {
            int ix = i - start;
            if (contains(i)) {
                int nxt = this.lastContiguous(i);
                if (nxt == i) {
                    result[i] = true;
                } else {
                    int tail = Math.min(end, nxt);
                    Arrays.fill(result, ix, tail, true);
                }
            }
        }
//
//        int[] cursor = new int[0];
//        int max = Math.min(end, size());
//        for (int i = start; i < max; i++) {
//            if (contains(i)) {
//                result[cursor[0]] = true;
//            }
//            cursor[0]++;
//        }
        return result;
    }

    @Override
    public void trim() {
        // do nothing
    }

    /**
     * For use when using an IntSet to manage a pool of objects in an array, or
     * similar: Find the last number in this set, of a run of sequential numbers
     * starting at zero (the first unused slot).
     *
     * @return The last contiguous number encountered, or -1 if none.
     */
    public int lastContiguous() {
        return lastContiguous(0);
    }

    /**
     * For use when using an IntSet to manage a pool of objects in an array, or
     * similar: Find the last number in this set, of a run of sequential numbers
     * starting at <code>startingAt</code> (the first unused slot).
     *
     * @param startingAt The starting value
     * @return The last contiguous number encountered, or -1 if none.
     */
    public int lastContiguous(int startingAt) {
        if (isEmpty()) {
            return -1;
        }
        int max = max();
        if (startingAt > max) {
            return startingAt;
        }
        if (startingAt < first()) {
            return startingAt >= 0 ? startingAt : -1;
        }
        PrimitiveIterator.OfInt it = iterator();
        int lastValue = it.next();
        do {
            if (lastValue == startingAt) {
                break;
            }
            if (it.hasNext()) {
                lastValue = it.next();
            }
        } while (lastValue < startingAt && it.hasNext());
        while (it.hasNext()) {
            int nextValue = it.next();
            if (nextValue != lastValue + 1) {
                return lastValue;
            }
            lastValue = nextValue;
        }
        return startingAt;
    }

    private static boolean anyArrayBased(Iterable<IntSet> all) {
        for (IntSet is : all) {
            if (is.isArrayBased()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Combine multiple IntSets via logical OR.
     *
     * @param all A collection of IntSets
     * @return A new IntSet
     */
    public static IntSet merge(Iterable<IntSet> all) {
        if (anyArrayBased(all)) {
            IntSetArray arr = new IntSetArray();
            for (IntSet is : all) {
                arr.addAll(is);
            }
            return arr;
        }
        BitSet bits = null;
        for (IntSet i : all) {
            if (bits == null) {
                bits = (BitSet) i.toBits();
            } else {
                bits.or(bits);
            }
        }
        return bits == null ? new IntSetImpl(1) : new IntSetImpl(bits);
    }

    /**
     * Combine multiple IntSets via logical AND.
     *
     * @param all A collection of IntSets
     * @return A new IntSet
     */
    public static IntSet intersection(Iterable<IntSet> all) {
        if (anyArrayBased(all)) {
            Iterator<IntSet> iter = all.iterator();
            if (!iter.hasNext()) {
                return IntSet.EMPTY;
            }
            IntSetArray initial = new IntSetArray(iter.next());
            while (iter.hasNext()) {
                initial.retainAll(iter.next());
                if (initial.isEmpty()) {
                    break;
                }
            }
            return initial;
        }
        BitSet bits = null;
        for (IntSet i : all) {
            if (bits == null) {
                bits = (BitSet) i.toBits();
            } else {
                bits.and(bits);
            }
        }
        return bits == null ? new IntSetImpl(1) : new IntSetImpl(bits);
    }

    /**
     * Allows internal implementations raw access to the underlying BitSet (or
     * one created on the fly by array-based implementations).
     *
     * @return A bit set
     */
    abstract BitSet bitsUnsafe();

    /**
     * Visit each integer value in the set with an IntConsumer.
     *
     * @param c The consumer
     */
    public final void forEachInt(IntConsumer c) {
        forEach(c);
    }

    /**
     * Get a new IntSet represeting the intersection of this and another IntSet.
     *
     * @param other Another IntSet
     * @return A new IntSet
     */
    public IntSet intersection(IntSet other) {
        if (other == this) {
            return copy();
        }
        BitSet b1 = this.bitsUnsafe();
        BitSet nue = (BitSet) other.bitsUnsafe().clone();
        nue.and(b1);
        return new IntSetImpl(nue);
    }

    /**
     * Get a new IntSet representing the combination of this and another.
     *
     * @param other
     * @return
     */
    public IntSet or(IntSet other) {
        if (other == this) {
            return copy();
        }
        BitSet b1 = this.bitsUnsafe();
        BitSet nue = (BitSet) other.toBits();
        nue.or(b1);
        return new IntSetImpl(nue);
    }

    /**
     * Get a new IntSet representing those integers which are only in either
     * this one or the passed one.
     *
     * @param other Another set
     * @return A new set, or the empty set
     */
    public IntSet xor(IntSet other) {
        if (other == this) {
            return EMPTY;
        }
        BitSet b1 = this.bitsUnsafe();
        BitSet nue = (BitSet) other.toBits();
        nue.xor(b1);
        return new IntSetImpl(nue);
    }

    /**
     * Add an array of ints to this IntSet.
     *
     * @param ints An array of ints
     * @return this
     */
    public IntSet addAll(int... ints) {
        for (int i : ints) {
            add(i);
        }
        return this;
    }

    /**
     * Returns a <i>copy</i> of this IntSet's internal state, modifications to
     * which shall not affect this set; when using array-based implementations,
     * this may throw an exception, or if large values are present, allocate
     * enormous bit sets.
     *
     * @return A bit set
     */
    public abstract BitSet toBits();

    /**
     * Add one integer value to this set.
     *
     * @param val The value
     * @return true if the collection was modified
     */
    public final boolean add(int val) {
        return _add(val);
    }

    abstract boolean _add(int val);

    /**
     * Randomly choose an integer from this set.
     *
     * @param r A random
     * @return An integer
     * @throws IndexOutOfBoundsException if the set is empty
     */
    public int pick(Random r) {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("Empty");
        }
        int max = bitsUnsafe().length();
        int pos = r.nextInt(max);
        int result = bitsUnsafe().previousSetBit(pos);
        if (result == -1) {
            result = bitsUnsafe().nextSetBit(pos);
        }
        return result;
    }

    /**
     * Optimized contents-equality test.
     *
     * @param other Another set
     * @return True if the contents are the same
     */
    public boolean sameContents(Set<? extends Integer> other) {
        if (size() != other.size()) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (other instanceof IntSet) {
            return bitsUnsafe().equals(((IntSet) other).bitsUnsafe());
        }
        BitSet matched = (BitSet) bitsUnsafe().clone();
        for (Integer o : other) {
            matched.clear(o);
        }
        return matched.cardinality() == 0;
    }

    /**
     * Remove the last (greatest) element in this set.
     *
     * @return the value removed
     */
    public abstract int removeLast();

    /**
     * Visit each integer in ascending order.
     *
     * @param cons A consumer
     * @deprecated collides with the method from Collection - use
     * <code>forEachInt()</code> instead
     */
    @Deprecated
    public abstract void forEach(IntConsumer cons);

    /**
     * Visit each integer in descending order
     *
     * @param cons A consumer
     */
    public abstract void forEachReversed(IntConsumer cons);

    /**
     * Get the contents as an integer array.
     *
     * @return An integer array
     */
    public abstract int[] toIntArray();

    /**
     * Cast or copy a Set&lt;Integer&gt; to an IntSet.
     *
     * @param set A set
     * @return An IntSet
     */
    public static IntSet of(Set<Integer> set) {
        if (set instanceof IntSet) {
            return ((IntSet) set);
        } else {
            return create(set);
        }
    }

    /**
     * Create the inverse of this IntSet - any values between <code>0</code> and
     * <code>upperBound</code> which are set in this IntSet will be unset in the
     * result and vice-versa.
     *
     * @param upperBound The upper bound
     * @return A set
     */
    public IntSet inverse(int upperBound) {
        return inverse(0, upperBound);
    }

    /**
     * Create the inverse of a range of this IntSet - any values between
     * <code>lowerBound</code> and <code>upperBound</code> which are set in this
     * IntSet will be unset in the result and vice-versa.
     *
     * @param the lower bound
     * @param upperBound The upper bound
     * @return A set
     */
    public IntSet inverse(int lowerBound, int upperBound) {
        IntSet result = create(upperBound - lowerBound);
        for (int i = lowerBound; i < result.first(); i++) {
            result.add(i);
        }
        result.removeAll(this);
        return result;
    }

    /**
     * Remove a range of values from this set.
     *
     * @param start The start (inclusive)
     * @param end The end (exclusive)
     * @return True if the set was altered
     */
    public boolean removeRange(int start, int end) {
        int min = Math.min(start, end);
        int max = Math.max(start, end);
        if (min == max) {
            return remove(min);
        }
        IntSet toRemove = IntSet.allOf(min, max);
        return removeAll(toRemove);
    }

    /**
     * Get the index of a given value in the sorted iteration sequence of this
     * set (more expensive for bitset-based sets).
     *
     * @param value An index or -1
     * @return The index
     */
    public abstract int indexOf(int value);

    /**
     * Consumer for values and indices.
     */
    public interface IntSetValueConsumer {

        /**
         * Called with a value.
         *
         * @param index The index within the map's sorted iteration order
         * @param value The value at that index
         */
        void onValue(int index, int value);
    }

    /**
     * Get the set of values between the first and last values, inclusive.
     *
     * @param first The first value, inclusive
     * @param last The last value, inclusive
     * @param bi A consumer which will be passed the values
     * @return The number of values that were passed to the consumer
     */
    public int valuesBetween(int first, int last, IntSetValueConsumer bi) {
        if (isEmpty()) {
            return 0;
        }
        int min = Math.min(first, last);
        int max = Math.max(first, last);
        if (max == min) {
            int ix = indexOf(first);
            if (ix >= 0) {
                bi.onValue(ix, first);
                return 1;
            }
            return 0;
        }
        int firstIx = nearestIndexTo(first, Bias.FORWARD);
        int lastIx = nearestIndexTo(last, Bias.BACKWARD);
        int maxIx = Math.max(firstIx, lastIx);
        int minIx = Math.min(firstIx, lastIx);
        for (int i = minIx; i <= maxIx; i++) {
            int val = valueAt(i);
            bi.onValue(i, val);
        }
        return (maxIx - minIx) + 1;
    }

    /**
     * Get the index of any value present in this set which is closest to the
     * passed value, taking into account the passed Bias.
     *
     * @param value The target value
     * @param bias The bias, to search forward, backward, neither, or prefer the
     * value, forward or backward, which is the least numeric distance to the
     * passed value
     * @return An integer index or -1
     */
    public abstract int nearestIndexTo(int value, Bias bias);

    /**
     * Get the nearest value present to a given value; note that this is
     * slightly fraught, since the behavior when the query cannot be satisfied
     * differs between array- and BitSet-based implementations.
     *
     * @param value The target value
     * @param bias The bias, to search forward, backward, neither, or prefer the
     * value, forward or backward, which is the least numeric distance to the
     * passed value
     * @return The nearest value, if present; array-based implementations will
     * return Integer.MIN_VALUE as a null result; bitset based implementations
     * use the more commonplace -1 for that purpose, since they cannot contain
     * negative values
     */
    public abstract int nearestValueTo(int value, Bias bias);

    /**
     * Safely pick an element from this set which is not also contained in the
     * passed set, adding it to that set.
     *
     * @param r A random
     * @param notIn A set of previously picked values
     * @return An integer, or null if the passed set contains all values in this
     * one
     */
    public Integer pick(Random r, IntSet notIn) {
        if (notIn.containsAll(this)) {
            return null;
        }
        IntSet sub = copy();
        sub.removeAll(notIn);
        if (sub.isEmpty()) {
            return null;
        }
        int result = sub.pick(r);
        notIn.add(result);
        return result;
    }

    public Integer pick(Random r, Set<Integer> notIn) {
        // XXX this is hideous and non-performant
        if (notIn.size() >= size()) {
            return null;
        }
        int result = pick(r);
        BitSet bits = bitsUnsafe();
        if (result == -1 || notIn.contains(result)) {
            int len = bits.length();
            int pos = r.nextInt(bits.length());
            int origPos = pos;
            int direction = 1;
            if (pos > len / 2) {
                direction = -1;
            }
            if (bits.get(pos) && !notIn.contains(pos)) {
                return pos;
            }
            boolean flipped = false;
            for (;;) {
                result = direction == -1 ? bits.previousSetBit(pos + direction)
                        : bits.nextSetBit(pos + direction);
                if (result == -1) {
                    if (flipped) {
                        result = -1;
                        break;
                    }
                    flipped = true;
                    direction *= -1;
                    pos = origPos;
                }
                if (!notIn.contains(result)) {
                    break;
                }
                pos = result;
            }
        }
        if (result == -1) {
            return null;
        } else {
            notIn.add(result);
            return result;
        }
    }

    /**
     * Get the first/least element in this set.
     *
     * @return The first element
     * @throws IndexOutOfBoundsException if the set is empty
     */
    public int first() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("Empty");
        }
        return bitsUnsafe().nextSetBit(0);
    }

    /**
     * Get the last/greatest element in this set.
     *
     * @return The first element
     * @throws IndexOutOfBoundsException if the set is empty
     */
    public int last() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("Empty");
        }
        return bitsUnsafe().previousSetBit(Integer.MAX_VALUE);
    }

    /**
     * Get the maximum value in this set (for all implementations in this
     * library, this is identical to calling <code>last()</code>).
     *
     * @return
     */
    public int max() {
        return last();
    }

    /**
     * Remove all values in the passed IntSet.
     *
     * @param ints An IntSet
     * @return
     */
    public boolean removeAll(IntSet ints) {
        if (ints == this) {
            boolean wasEmpty = isEmpty();
            clear();
            return !wasEmpty;
        }
        boolean[] result = new boolean[1];
        ints.forEachReversed(val -> {
            result[0] |= remove(val);
        });
        return result[0];
    }

    /**
     * Remove an int from this set.
     *
     * @param value The value
     * @return True if the collection was modified.
     */
    public abstract boolean remove(int value);

    /**
     * Remove the first/least value in this collection.
     *
     * @return the removed value
     */
    public abstract int removeFirst();

    /**
     * Determine if this set contains the passed integer.
     *
     * @param val The integer
     * @return True if it is present
     */
    public abstract boolean contains(int val);

    /**
     * Empty this set, setting its size to zero.
     */
    @Override
    public abstract void clear();

    /**
     * Create an independent copy of this set.
     *
     * @return A new set
     */
    public abstract IntSet copy();

    /**
     * Create a read-only view (which will still reflect changes in this one,
     * a-la Collections.unmodifiableSet()).
     *
     * @return An IntSet whose mutation methods throw
     * <code>UnsupportedOperationException</code>
     */
    public IntSet readOnlyView() {
        return new IntSetReadOnly(this);
    }

    /**
     * Overloads <code>Set.iterator()</code> to return
     * <code>PrimitiveIterator.OfInt</code>.
     *
     * @return An iterator
     */
    @Override
    public abstract PrimitiveIterator.OfInt iterator();
    
    public IntList toList() {
        return IntList.createFrom(toIntArray());
    }

    /**
     * An immutable empty IntSet instance.
     */
    public static final IntSet EMPTY = new Empty();

    private static final class Empty extends IntSet implements PrimitiveIterator.OfInt {

        @Override
        public String toString() {
            return "[]";
        }

        @Override
        public void trim() {
            // do nothing
        }

        @Override
        public int indexOf(int value) {
            return -1;
        }

        @Override
        public int nearestIndexTo(int value, Bias bias) {
            return -1;
        }

        @Override
        public int nearestValueTo(int value, Bias bias) {
            return -1;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null) {
                return false;
            } else if (o instanceof IntSet) {
                return ((IntSet) o).isEmpty();
            } else if (o instanceof Collection<?>) {
                return ((Collection<?>) o).isEmpty();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        BitSet bitsUnsafe() {
            return new BitSet(0);
        }

        @Override
        public IntSet or(IntSet other) {
            return other.copy();
        }

        @Override
        public IntSet copy() {
            return this;
        }

        @Override
        public IntSet xor(IntSet other) {
            return new IntSetImpl().xor(other);
        }

        @Override
        public IntSet addAll(int... ints) {
            throw new UnsupportedOperationException("Immutable.");
        }

        @Override
        public BitSet toBits() {
            return new BitSet(1);
        }

        @Override
        boolean _add(int val) {
            throw new UnsupportedOperationException("Immutable.");
        }

        @Override
        public boolean remove(int val) {
            throw new UnsupportedOperationException("Immutable.");
        }

        @Override
        public int first() {
            return -1;
        }

        @Override
        public int removeFirst() {
            throw new UnsupportedOperationException("Immutable.");
        }

        @Override
        public int removeLast() {
            throw new UnsupportedOperationException("Immutable.");
        }

        @Override
        public void forEach(IntConsumer cons) {
            // do nothing
        }

        @Override
        public void forEachReversed(IntConsumer cons) {
            // do nothing
        }

        @Override
        public int[] toIntArray() {
            return new int[0];
        }

        @Override
        public int last() {
            throw new NoSuchElementException("Empty.");
        }

        @Override
        public boolean contains(int val) {
            return false;
        }

        @Override
        public void clear() {
            // do nothing
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public PrimitiveIterator.OfInt iterator() {
            return this;
        }

        @Override
        public Object[] toArray() {
            return new Integer[0];
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            return (T[]) new Object[0];
        }

        @Override
        public boolean add(Integer e) {
            throw new UnsupportedOperationException("Immutable.");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("Immutable.");
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return c.isEmpty();
        }

        @Override
        public boolean addAll(Collection<? extends Integer> c) {
            throw new UnsupportedOperationException("Immutable.");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("Immutable.");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("Immutable.");
        }

        @Override
        public int nextInt() {
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasNext() {
            return false;
        }
    }

    /**
     * Determine if this instance is array-based.
     *
     * @return
     */
    public boolean isArrayBased() {
        return this instanceof IntSetArray
                || this instanceof IntSetReadOnly
                && ((IntSetReadOnly) this).delegate.isArrayBased();
    }

    /**
     * Visit sets of consecutive items within this set, in reverse order (useful
     * for batching changes in arrays).
     *
     * @param v A visitor which will be passed the start and end indices of each
     * segment of contiguous numbers
     * @return The number of such segments visited
     */
    public int visitConsecutiveIndicesReversed(ConsecutiveItemsVisitor v) {
        int sz = size();
        if (sz == 0) {
            return 0;
        } else if (sz == 1) {
            v.items(0, 0, 1);
            return 1;
        }
        int[] arr = toIntArray();
        int total = 0;
        int endIx = arr.length - 1;
        int next = arr[arr.length - 1];
        for (int i = arr.length - 2; i >= 0; i--) {
            int curr = arr[i];
            if (next != curr + 1) {
                v.items(i + 1, endIx, endIx - i);
                total++;
                endIx = i;
            }
            next = curr;
            if (i == 0) {
                v.items(0, endIx, endIx + 1);
                total++;
            }
        }
        return total;
    }

    /**
     * Visit sets of consecutive items within this set, in reverse order (useful
     * for batching changes in arrays).
     *
     * @param v A visitor which will be passed the start and end indices of each
     * segment of contiguous numbers
     * @return The number of such segments visited
     */
    public int visitConsecutiveIndices(ConsecutiveItemsVisitor v) {
        int sz = size();
        if (sz == 0) {
            return 0;
        } else if (sz == 1) {
            v.items(0, 0, iterator().nextInt());
            return 1;
        }
        int[] arr = toIntArray();
        int startIx = 0;
        int count = 1;
        int prev = arr[0];
        int total = 0;
        for (int i = 1; i < arr.length; i++) {
            int curr = arr[i];
            if (curr - prev == 1) {
                count++;
            } else {
                v.items(startIx, startIx + count - 1, count);
                count = 1;
                startIx = i;
                total++;
            }
            prev = curr;
            if (i == arr.length - 1) {
                v.items(startIx, startIx + count - 1, count);
                total++;
            }
        }
        return total;
    }

    /**
     * Create a copy of this set as an IntList.
     *
     * @return A list
     */
    public IntList asList() {
        return new IntListImpl(toIntArray());
    }

    /**
     * Array-like access by index; note that for BitSet-based implementations
     * this is expensive; for array-based implementations it is much cheaper
     * than performing a search.
     *
     * @param index The index of the value
     * @return The value in the sort order of this set, low to high.
     */
    public int valueAt(int index) {
        return toIntArray()[index];
    }

    /**
     * {@inheritDoc }
     *
     * Overridden to return Spliterator.OfInt.
     *
     * @return A spliterator
     */
    @Override
    public Spliterator.OfInt spliterator() {
        return new ArrayIntSpliterator(toIntArray());
    }
}
