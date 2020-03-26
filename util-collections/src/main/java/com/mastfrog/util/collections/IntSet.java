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

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.Set;
import java.util.function.IntConsumer;
//import jdk.internal.HotSpotIntrinsicCandidate;

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
public abstract class IntSet implements Set<Integer>, Cloneable {

//    @HotSpotIntrinsicCandidate
    IntSet() {
    }

    public static IntSet bitSetBased(int capacity) {
        return new IntSetImpl(capacity);
    }

    public static IntSet arrayBased(int capacity) {
        return new IntSetArray(capacity);
    }

    public static IntSet create(int capacity) {
        int targetBytes = capacity / Long.BYTES;
        if (targetBytes > 1073741824) {
            return new IntSetArray(capacity);
        }
        return new IntSetImpl(capacity);
    }

    public static IntSet create(int minValue, int maxValue) {
        int len = (maxValue - minValue) + 1;
        if (len > 1073741824 || minValue < 0) {
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

    public static IntSet create(int[] arr) {
        BitSet set = new BitSet(arr.length);
        for (int i = 0; i < arr.length; i++) {
            set.set(arr[i]);
        }
        return new IntSetImpl(set);
    }

    public static IntSet create(BitSet bits) {
        return new IntSetImpl((BitSet) bits.clone());
    }

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

    private static boolean anyArrayBased(Iterable<IntSet> all) {
        boolean result = false;
        for (IntSet is : all) {
            if (is.isArrayBased()) {
                return true;
            }
        }
        return false;
    }

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

    abstract BitSet bitsUnsafe();

    public final void forEachInt(IntConsumer c) {
        forEach(c);
    }

    public IntSet intersection(IntSet other) {
        BitSet b1 = this.bitsUnsafe();
        BitSet nue = (BitSet) other.bitsUnsafe().clone();
        nue.and(b1);
        return new IntSetImpl(nue);
    }

    public IntSet or(IntSet other) {
        BitSet b1 = this.bitsUnsafe();
        BitSet nue = (BitSet) other.toBits();
        nue.or(b1);
        return new IntSetImpl(nue);
    }

    public IntSet xor(IntSet other) {
        BitSet b1 = this.bitsUnsafe();
        BitSet nue = (BitSet) other.toBits();
        nue.xor(b1);
        return new IntSetImpl(nue);
    }

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

    public final boolean add(int val) {
        return _add(val);
    }

    abstract boolean _add(int val);

    public int pick(Random r) {
        int max = bitsUnsafe().length();
        int pos = r.nextInt(max);
        int result = bitsUnsafe().previousSetBit(pos);
        if (result == -1) {
            result = bitsUnsafe().nextSetBit(pos);
        }
        return result;
    }

    public boolean sameContents(Set<Integer> other) {
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

    public abstract int removeLast();

    /**
     * Visit each integer
     *
     * @param cons A consumer
     * @deprecated collides with the method from Collection - use
     * <code>forEachInt()</code> instead
     */
    @Deprecated
    public abstract void forEach(IntConsumer cons);

    public abstract void forEachReversed(IntConsumer cons);

    public abstract int[] toIntArray();

    public Integer pick(Random r, Set<Integer> notIn) {
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

    public int first() {
        return bitsUnsafe().nextSetBit(0);
    }

    public int last() {
        return bitsUnsafe().previousSetBit(Integer.MAX_VALUE);
    }

    public int max() {
        int sz = bitsUnsafe().size();
        return bitsUnsafe().previousSetBit(sz);
    }

    public boolean removeAll(IntSet ints) {
        boolean[] result = new boolean[1];
        ints.forEachReversed(val -> {
            result[0] |= remove(val);
        });
        return result[0];
    }

    public abstract boolean remove(int bit);

    public abstract int removeFirst();

    public abstract boolean contains(int val);

    @Override
    public abstract void clear();

    /**
     * Create an independent copy of this set.
     *
     * @return A new set
     */
    public abstract IntSet copy();

    public IntSet readOnlyView() {
        return new IntSetReadOnly(this);
    }

    @Override
    public abstract PrimitiveIterator.OfInt iterator();

    public static final IntSet EMPTY = new Empty();

    private static final class Empty extends IntSet implements PrimitiveIterator.OfInt {

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

    public boolean isArrayBased() {
        return this instanceof IntSetArray;
    }

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

}
