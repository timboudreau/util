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

/**
 * A Java set of integers backed by a BitSet, which can look up random elements.
 * For performance, this is considerably faster than using a standard Java set
 * with a list maintained along side it for random access <i>IF the range from
 * lowest to highest integer in the set is small</i>.
 * <p>
 * Does not support negative integer values.
 * </p>
 *
 * @author Tim Boudreau
 */
import java.util.BitSet;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.Set;
import java.util.function.IntConsumer;
//import jdk.internal.HotSpotIntrinsicCandidate;

/**
 * A set of primitive integers based on a BitSet, which implements
 * <code>Set&lt;Integer&gt;</code> for convenience. For performance, this is
 * considerably faster than using a standard Java set with a list maintained
 * along side it for random access <i>IF the range from lowest to highest
 * integer in the set is small</i>.
 * <p>
 * <b>Does not support negative integer values.</b>
 * </p>
 *
 *
 * @author Tim Boudreau
 */
public abstract class IntSet implements Set<Integer>, Cloneable {

//    @HotSpotIntrinsicCandidate
    IntSet() {
    }

    public static IntSet create(int capacity) {
        return new IntSetImpl(capacity);
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

    public static IntSet merge(Iterable<IntSet> all) {
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
     * which shall not affect this set.
     *
     * @return A bit set
     */
    public abstract BitSet toBits();

    public final boolean add(int val) {
        if (val < 0) {
            throw new IllegalArgumentException("Negative values not allowed");
        }
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

    public abstract boolean remove(int bit);

    public abstract int removeFirst();

    public abstract boolean contains(int val);

    @Override
    public abstract void clear();

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
            return other;
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
}
