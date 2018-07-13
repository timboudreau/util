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

import java.lang.reflect.Array;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

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
public final class IntSet implements Set<Integer> {

    private final BitSet bits;

    public IntSet() {
        this(96);
    }

    public IntSet(int capacity) {
        bits = new BitSet(capacity);
    }

    public IntSet(Set<Integer> other) {
        this(other.size());
        addAll(other);
    }

    IntSet(BitSet bits) {
        this.bits = bits;
    }
    
    public static IntSet intersection(Iterable<IntSet> all) {
        BitSet bits = null;
        for (IntSet i : all) {
            if (bits == null) {
                bits = (BitSet) i.bits.clone();
            } else {
                bits.and(bits);
            }
        }
        return bits == null ? new IntSet(1) : new IntSet(bits);
    }
    
    public static IntSet merge(Iterable<IntSet> all) {
        BitSet bits = null;
        for (IntSet i : all) {
            if (bits == null) {
                bits = (BitSet) i.bits.clone();
            } else {
                bits.or(bits);
            }
        }
        return bits == null ? new IntSet(1) : new IntSet(bits);
    }    

    public IntSet intersection(IntSet other) {
        BitSet b1 = this.bits;
        BitSet nue = (BitSet) other.bits.clone();
        nue.and(b1);
        return new IntSet(nue);
    }

    public IntSet or(IntSet other) {
        BitSet b1 = this.bits;
        BitSet nue = (BitSet) other.bits.clone();
        nue.or(b1);
        return new IntSet(nue);
    }

    public IntSet xor(IntSet other) {
        BitSet b1 = this.bits;
        BitSet nue = (BitSet) other.bits.clone();
        nue.xor(b1);
        return new IntSet(nue);
    }

    public IntSet addAll(int... ints) {
        for (int i : ints) {
            add(i);
        }
        return this;
    }

    public BitSet toBits() {
        BitSet result = new BitSet(bits.size());
        result.or(bits);
        return result;
    }

    public boolean add(int val) {
        if (val < 0) {
            throw new IllegalArgumentException("Bit set cannot support negative"
                    + " indices");
        }
        boolean result = !bits.get(val);
        if (result) {
            bits.set(val);
        }
        return result;
    }

    public boolean remove(int val) {
        boolean result = bits.get(val);
        bits.clear(val);
        return result;
    }

    public int pick(Random r) {
        int max = bits.length();
        int pos = r.nextInt(max);
        int result = bits.previousSetBit(pos);
        if (result == -1) {
            result = bits.nextSetBit(pos);
        }
        return result;
    }

    public boolean sameContents(Set<Integer> other) {
        return toIntSet(other).equals(this);
    }

    private IntSet toIntSet(Set<Integer> set) {
        if (set instanceof IntSet) {
            return (IntSet) set;
        } else {
            return new IntSet(set);
        }
    }

    public int first() {
        return bits.nextSetBit(0);
    }

    public int last() {
        return bits.previousSetBit(Integer.MAX_VALUE);
    }

    public int removeFirst() {
        int pos = bits.nextSetBit(0);
        if (pos != -1) {
            bits.clear(pos);
        }
        return pos;
    }

    public Integer pick(Random r, Set<Integer> notIn) {
        if (notIn.size() >= size()) {
            return null;
        }
        int result = pick(r);
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

    @Override
    public int size() {
        return bits.cardinality();
    }

    public int max() {
        int sz = bits.size();
        return bits.previousSetBit(sz);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean contains(int val) {
        return bits.get(val);
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof Integer && ((Integer) o) >= 0 && bits.get((Integer) o);
    }

    @Override
    public Iterator<Integer> iterator() {
        return new BitSetIterator(bits);
    }

    private static final class BitSetIterator implements Iterator<Integer> {

        int pos = 0;
        private final BitSet bits;

        public BitSetIterator(BitSet bits) {
            this.bits = bits;
        }

        @Override
        public boolean hasNext() {
            return pos < bits.length();
        }

        @Override
        public Integer next() {
            int result = bits.nextSetBit(pos);
            if (result == -1) {
                throw new IndexOutOfBoundsException("No more values");
            }
            pos = result + 1;
            return result;
        }
    }
    
    public Interator interator() {
        return new InteratorImpl(bits);
    }
    
    
    private static class InteratorImpl implements Interator {
        int pos = 0;
        final BitSet bits;

        public InteratorImpl(BitSet bits) {
            this.bits = bits;
        }

        @Override
        public boolean hasNext() {
            return pos < bits.length();
        }

        @Override
        public int next() {
            int result = bits.nextSetBit(pos);
            if (result == -1) {
                throw new IndexOutOfBoundsException("No more values");
            }
            pos = result + 1;
            return result;
        }
    }

    @Override
    public Object[] toArray() {
        Object[] result = new Object[size()];
        for (int curr = bits.nextSetBit(0), i = 0; curr != -1; i++, curr = bits.nextSetBit(curr + 1)) {
            result[i] = curr;
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] result) {
        if (result.length < size()) {
            result = (T[]) Array.newInstance(result.getClass().getComponentType(), size());
        }
        for (int curr = bits.nextSetBit(0), i = 0; curr != -1; i++, curr = bits.nextSetBit(curr + 1)) {
            result[i] = (T) Integer.valueOf(curr);
        }
        return result;
    }

    public int[] toIntArray() {
        int[] result = new int[size()];
        for (int curr = bits.nextSetBit(0), i = 0; curr != -1; i++, curr = bits.nextSetBit(curr + 1)) {
            result[i] = curr;
        }
        return result;
    }

    @Override
    public boolean add(Integer e) {
        return add(e.intValue());
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof Integer) {
            return remove(((Integer) o).intValue());
        }
        return false;
    }

    @Override
    public boolean containsAll(
            Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
            if (o instanceof Integer) {
                int val = ((Integer) o);
                result = contains(val);
                if (!result) {
                    break;
                }
            } else {
                return false;
            }
        }
        return result;
    }

    @Override
    public boolean addAll(
            Collection<? extends Integer> c) {
        boolean result;
        if (c instanceof IntSet) {
            int old = size();
            bits.or(((IntSet) c).bits);
            result = size() != old;
        } else {
            BitSet bs = new BitSet();
            for (Integer i : c) {
                int val = i;
                if (!bs.get(val)) {
                    bs.set(val);
                }
            }
            result = !bs.isEmpty();
            if (result) {
                bits.and(bs);
            }
        }
        return result;
    }

    @Override
    public boolean retainAll(
            Collection<?> c) {
        if (c instanceof IntSet) {
            int old = bits.cardinality();
            bits.and(((IntSet) c).bits);
            return bits.cardinality() != old;
        } else {
            BitSet bs = new BitSet();
            for (Object o : c) {
                if (o instanceof Integer) {
                    bs.set(((Integer) o));
                }
            }
            boolean result = !bs.isEmpty();
            if (result) {
                bits.and(bs);
            }
            return result && !bits.isEmpty();
        }
    }

    @Override
    public boolean removeAll(
            Collection<?> c) {
        if (c instanceof IntSet) {
            int old = bits.cardinality();
            bits.andNot(((IntSet) c).bits);
            return old != bits.cardinality();
        } else {
            BitSet bs = new BitSet();
            for (Object o : c) {
                if (o instanceof Integer) {
                    bs.set(((Integer) o));
                }
            }
            int size = size();
            bits.andNot(bs);
            return size != size();
        }
    }

    @Override
    public void clear() {
        bits.clear();
    }

    @Override
    public int hashCode() {
        //follows the contract of AbstractSet.hashCode()
        int h = 0;
        for (int curr = bits.nextSetBit(0), i = 0; curr != -1; i++, curr = bits.nextSetBit(curr + 1)) {
            h += curr;
        }
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o instanceof IntSet) {
            return ((IntSet) o).bits.equals(bits);
        } else if (o instanceof Iterable) {
            BitSet bs = new BitSet(size());
            Iterable<?> it = (Iterable<?>) o;
            for (Object elem : it) {
                if (elem instanceof Integer) {
                    bs.set(((Integer) elem));
                } else {
                    return false;
                }
            }
            return bs.equals(bits);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int curr = bits.nextSetBit(0), i = 0; curr != -1; i++, curr = bits.nextSetBit(curr + 1)) {
            sb.append(curr);
            if (bits.nextSetBit(curr + 1) != -1) {
                sb.append(',');
            }
        }
        return sb.toString();
    }
}
