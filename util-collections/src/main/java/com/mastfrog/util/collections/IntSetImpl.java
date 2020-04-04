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

import com.mastfrog.util.search.Bias;
import java.lang.reflect.Array;
import java.util.Arrays;
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

    IntSetImpl(IntSetImpl other) {
        this.bits = (BitSet) other.bits.clone();
    }

    @Override
    public IntSet copy() {
        return new IntSetImpl(this);
    }

    @Override
    BitSet bitsUnsafe() {
        return bits;
    }

    @Override
    public boolean[] toBooleanArray(int size) {
        boolean[] result = new boolean[size];
        visitConsecutiveIndices((int first, int last, int count) -> {
            Arrays.fill(result, first, last + 1, true);
        });
        return result;
    }

    @Override
    public IntSet intersection(IntSet other) {
        if (other.isArrayBased()) {
            BitSet bs = new BitSet(size());
            other.forEachInt(val -> {
                if (bits.get(val)) {
                    bs.set(val);
                }
            });
            return new IntSetImpl(bs);
        }
        BitSet nue = (BitSet) other.bitsUnsafe();
        nue.and(this.bits);
        return new IntSetImpl(nue);
    }

    private IntSetArray toArrayBased() {
        IntSetArray arr = new IntSetArray(size());
        forEachInt(val -> {
            arr.add(val);
        });
        return arr;
    }

    public IntSet or(IntSet other) {
        if (other.isArrayBased()) {
            IntSetArray arr = toArrayBased();
            return arr.or(other);
        }
        BitSet nue = (BitSet) other.toBits();
        nue.or(this.bits);
        return new IntSetImpl(nue);
    }

    public IntSet xor(IntSet other) {
        if (other.isArrayBased()) {
            IntSetArray arr = toArrayBased();
            return arr.xor(other);
        }
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

    @Override
    public int max() {
        return last();
    }

    @Override
    public int lastContiguous(int startingAt) {
        if (startingAt < 0) {
            startingAt = 0;
        }
        if (!bits.get(startingAt)) {
            return startingAt;
        }
        int result = bits.nextClearBit(startingAt);
        if (result >= 0) {
            return result - 1;
        }
        return -1;
    }

    @Override
    public int valuesBetween(int start, int end, IntSetValueConsumer bi) {
        if (start == end) {
            if (contains(start)) {
                bi.onValue(indexOf(start), start);
                return 1;
            }
            return 0;
        }
        int realStart = bits.nextSetBit(start);
        int realEnd = bits.previousSetBit(end);
        if (realStart > realEnd) {
            return 0;
        }
        int index = indexOf(realStart);
        int curr = realStart;
        int count = 0;
        do {
            int runEnd = bits.nextClearBit(curr);
            if (runEnd < 0) {
                break;
            }
            for (int i = curr; i < runEnd && i <= realEnd; i++) {
                bi.onValue(index++, i);
                count++;
            }
            curr = bits.nextSetBit(runEnd);
        } while (curr > 0 && curr <= realEnd);
        return count;
    }

    @Override
    public int indexOf(int value) {
        if (value < 0) {
            return -1;
        }
        if (!bits.get(value)) {
            return -1;
        }
        // okay we know the bit will be set
        if (value == 0) {
            // it is 0 and it is set so it can only be bit 0
            return 0;
        }
        int result = 0;
        int prev = value;
        do {
            int precedingUnset = bits.previousClearBit(prev - 1);
            if (precedingUnset < 0) {
                break;
            }
            result += prev - precedingUnset;
            prev = bits.previousSetBit(precedingUnset);
        } while (prev >= 0);
        return result - 1;
    }

    @Override
    public int nearestIndexTo(int value, Bias bias) {
        if (bits.get(value)) {
            return indexOf(value);
        }
        switch (bias) {
            case NONE:
                return -1;
            case BACKWARD:
                int prev = bits.previousSetBit(value);
                if (prev < 0) {
                    return -1;
                }
                return indexOf(prev);
            case FORWARD:
                int next = bits.nextSetBit(value);
                if (next < 0) {
                    return -1;
                }
                return indexOf(next);
            case NEAREST:
                int prev2 = bits.previousSetBit(value);
                int next2 = bits.nextSetBit(value);
                if (prev2 < 0) {
                    return next2 < 0 ? -1 : indexOf(next2);
                } else if (next2 < 0) {
                    return indexOf(prev2);
                }
                if (next2 - value < value - prev2) {
                    return indexOf(next2);
                } else {
                    return indexOf(prev2);
                }
            default:
                throw new AssertionError(bias);
        }
    }

    @Override
    public int nearestValueTo(int value, Bias bias) {
        if (bits.get(value)) {
            return value;
        }
        switch (bias) {
            case NONE:
                return -1;
            case BACKWARD:
                return bits.previousSetBit(value);
            case FORWARD:
                return bits.nextSetBit(value);
            case NEAREST:
                int prev = bits.previousSetBit(value);
                int next = bits.nextSetBit(value);
                if (prev < 0) {
                    return next;
                } else if (next < 0) {
                    return prev;
                }
                if (next - value < value - prev) {
                    return next;
                } else {
                    return prev;
                }
            default:
                throw new AssertionError(bias);
        }
    }

    @Override
    public IntSet inverse(int lowerBound, int upperBound) {
        if (lowerBound == upperBound) {
            return create(size());
        }
        int min = Math.min(lowerBound, upperBound);
        int max = Math.max(lowerBound, upperBound);
        BitSet inverse = new BitSet(Math.max(1, max - min));
        inverse.set(min, max);
        inverse.andNot(this.bits);
        int first = inverse.nextSetBit(0);
        if (first < min) {
            inverse.clear(0, min);
        }
        int last = inverse.previousSetBit(inverse.size());
        if (last > max) {
            inverse.clear(max, inverse.size());
        }
        return new IntSetImpl(inverse);
    }

    @Override
    public int pick(Random r) {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("Empty.");
        }
        int max = bits.length();
        int pos = r.nextInt(max);
        int result = bits.previousSetBit(pos);
        if (result == -1) {
            result = bits.nextSetBit(pos);
        }
        return result;
    }

    public boolean sameContents(Set<? extends Integer> other) {
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
    @SuppressWarnings("deprecation")
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
    public int valueAt(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index: " + index);
        }
        for (int curr = bits.nextSetBit(0), seen = 0; curr != -1; curr = bits.nextSetBit(curr + 1), seen++) {
            if (seen == index) {
                return curr;
            }
        }
        throw new IndexOutOfBoundsException("Index out of bounds: " + index + " of " + size());
    }

    @Override
    public int[] toIntArray() {
        int[] result = new int[size()];
        for (int curr = bits.nextSetBit(0), i = 0; curr != -1; curr = bits.nextSetBit(curr + 1), i++) {
            result[i] = curr;
        }
        return result;
    }

    @Override
    public boolean removeAll(IntSet ints) {
        if (ints == this) {
            clear();
        }
        if (!ints.isArrayBased()) {
            BitSet otherBits = ints.bitsUnsafe();
            int card = bits.cardinality();
            bits.andNot(otherBits);
            return card != bits.cardinality();
        }
        return super.removeAll(ints);
    }

    @Override
    public int visitConsecutiveIndices(ConsecutiveItemsVisitor v) {
        if (bits.isEmpty()) {
            return 0;
        }
        int first = bits.nextSetBit(0);
        int count = 0;
        int index = 0;
        do {
            int unset = bits.nextClearBit(first + 1);
            int distance = unset - first;
            v.items(index, index + distance - 1, distance);
            index += distance;
            count++;
            first = bits.nextSetBit(unset + 1);
        } while (first > 0);
        return count;
    }

    @Override
    public int visitConsecutiveIndicesReversed(ConsecutiveItemsVisitor v) {
        if (bits.isEmpty()) {
            return 0;
        }
        int last = bits.previousSetBit(Integer.MAX_VALUE);
        int index = bits.cardinality() - 1;
        int count = 0;
        do {
            int unset = bits.previousClearBit(last - 1);
            if (unset < 0) {
                v.items(0, last, last + 1);
                count++;
                break;
            }
            int distance = last - unset;
            v.items(index - (distance - 1), index, distance);
            index -= distance;
            count++;
            last = bits.previousSetBit(unset - 1);
            if (last == 0) {
                count++;
                v.items(0, 0, 1);
                break;
            }
        } while (last > 0);
        return count;
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
            IntSet is = (IntSet) c;
            int old = size();
            if (!is.isArrayBased()) {
                bits.or(((IntSet) c).bitsUnsafe());
                result = size() != old;
            } else {
                is.forEachInt(this::add);
                result = size() != old;
            }
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
        c = IntSetReadOnly.unwrap(c);
        if (c instanceof IntSet && !((IntSet) c).isArrayBased()) {
            int old = bits.cardinality();
            bits.and(((IntSet) c).bitsUnsafe());
            return bits.cardinality() != old;
        } else if (c instanceof IntSetArray) {
            int size = bits.cardinality();
            IntSetArray other = (IntSetArray) c;
            forEachInt(val -> {
                if (!other.contains(val)) {
                    remove(val);
                }
            });
            return size != bits.cardinality();
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
        c = IntSetReadOnly.unwrap(c);
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
        }
        if (o instanceof Collection<?>) {
            o = IntSetReadOnly.unwrap((Collection<?>) o);
        }
        if (o instanceof IntSetImpl) {
            return ((IntSetImpl) o).bits.equals(bits);
        } else if (o instanceof IntSet) {
            return Arrays.equals(((IntSet) o).toIntArray(), toIntArray());
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
