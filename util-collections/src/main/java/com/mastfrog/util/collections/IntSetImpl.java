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
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.Set;
import java.util.function.IntConsumer;

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
final class IntSetImpl extends IntSet {

    private final BitSet bits;

    IntSetImpl() {
        this(96);
    }

    IntSetImpl(int capacity) {
        bits = new BitSet(capacity);
    }

    IntSetImpl(Collection<? extends Integer> other) {
        this(other.size());
        addAll(other);
    }

    IntSetImpl(BitSet bits) {
        this.bits = bits;
    }

    BitSet bitsUnsafe() {
        return bits;
    }

    public IntSetImpl intersection(IntSetImpl other) {
        BitSet nue = (BitSet) other.bits;
        nue.and(this.bits);
        return new IntSetImpl(nue);
    }

    public IntSetImpl or(IntSet other) {
        BitSet nue = (BitSet) other.toBits();
        nue.or(this.bits);
        return new IntSetImpl(nue);
    }

    public IntSet xor(IntSet other) {
        BitSet nue = (BitSet) other.toBits();
        nue.xor(this.bits);
        return new IntSetImpl(nue);
    }

    @Override
    public IntSetImpl addAll(int... ints) {
        for (int i : ints) {
            add(i);
        }
        return this;
    }

    @Override
    public BitSet toBits() {
        return (BitSet) bits.clone();
    }

    @Override
    boolean _add(int val) {
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

    @Override
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
        if (size() != other.size()) {
            return false;
        }
        BitSet matched = (BitSet) bits.clone();
        for (Integer o : other) {
            matched.clear(o);
        }
        return matched.cardinality() == 0;
    }

    public int first() {
        return bits.nextSetBit(0);
    }

    @Override
    public int removeFirst() {
        int pos = bits.nextSetBit(0);
        if (pos != -1) {
            bits.clear(pos);
        }
        return pos;
    }

    @Override
    public int removeLast() {
        int pos = bits.previousSetBit(Integer.MAX_VALUE);
        if (pos != -1) {
            bits.clear(pos);
        }
        return pos;
    }

    @Override
    public void forEach(IntConsumer cons) {
        for (int curr = bits.nextSetBit(0); curr != -1; curr = bits.nextSetBit(curr + 1)) {
            cons.accept(curr);
        }
    }

    @Override
    public void forEachReversed(IntConsumer cons) {
        for (int curr = bits.previousSetBit(Integer.MAX_VALUE); curr != -1; curr = bits.previousSetBit(curr - 1)) {
            cons.accept(curr);
        }
    }

    @Override
    public int[] toIntArray() {
        int[] result = new int[size()];
        for (int curr = bits.nextSetBit(0), i = 0; curr != -1; curr = bits.nextSetBit(curr + 1), i++) {
            result[i] = curr;
        }
        return result;
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

    @Override
    public int last() {
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
    public PrimitiveIterator.OfInt iterator() {
        return new BitSetIterator(bits);
    }

    private static final class BitSetIterator implements Iterator<Integer>, PrimitiveIterator.OfInt {

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
        public int nextInt() {
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
            bits.or(((IntSet) c).bitsUnsafe());
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
                bits.or(bs);
            }
        }
        return result;
    }

    @Override
    public boolean retainAll(
            Collection<?> c) {
        if (c instanceof IntSetImpl) {
            int old = bits.cardinality();
            bits.and(((IntSetImpl) c).bits);
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
        if (c instanceof IntSetImpl) {
            int old = bits.cardinality();
            bits.andNot(((IntSetImpl) c).bits);
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
        } else if (o instanceof IntSetImpl) {
            return ((IntSetImpl) o).bits.equals(bits);
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
        StringBuilder sb = new StringBuilder(size() * 8).append('[');
        for (int curr = bits.nextSetBit(0); curr != -1; curr = bits.nextSetBit(curr + 1)) {
            sb.append(curr);
            if (bits.nextSetBit(curr + 1) != -1) {
                sb.append(", ");
            }
        }
        return sb.append(']').toString();
    }
}
