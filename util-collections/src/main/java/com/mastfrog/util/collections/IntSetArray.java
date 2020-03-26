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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.function.IntConsumer;

/**
 *
 * @author Tim Boudreau
 */
class IntSetArray extends IntSet {

    private int[] data;
    private int size;
    private boolean sorted = true;
    private final int initialCapacity;

    IntSetArray(int initialSize) {
        this.initialCapacity = Math.max(8, initialSize);
        data = new int[initialSize];
    }

    IntSetArray() {
        this(32);
    }

    IntSetArray(int[] data, int size, boolean sorted) {
        this.data = data;
        this.size = size;
        this.sorted = sorted;
        this.initialCapacity = data.length;
    }

    IntSetArray(IntSetArray other) {
        this.data = Arrays.copyOf(other.data, other.data.length);
        this.initialCapacity = other.initialCapacity;
        this.sorted = other.sorted;
        this.size = other.size;
    }

    IntSetArray(IntSet set) {
        if (set instanceof IntSetReadOnly) {
            set = ((IntSetReadOnly) set).delegate;
        }
        if (set instanceof IntSetArray) {
            IntSetArray other = (IntSetArray) set;
            this.data = Arrays.copyOf(other.data, other.data.length);
            this.initialCapacity = other.initialCapacity;
            this.sorted = other.sorted;
            this.size = other.size;
        } else {
            initialCapacity = set.size();
            data = new int[initialCapacity];
            size = 0;
            sorted = false;
            set.forEachInt(this::add);
        }
    }

    @Override
    public int valueAt(int index) {
        return data[index];
    }

    private void maybeGrow() {
        if (size + 1 == data.length) {
            data = Arrays.copyOf(data, data.length + initialCapacity + 1);
        }
    }

    private void maybeGrowToAccomodate(int count) {
        if (data.length <= size + count) {
            int newSize = Math.max(data.length, size + count)
                    + initialCapacity + 1;
            data = Arrays.copyOf(data, newSize);
        }
    }

    public int min() {
        if (size == 0) {
            return -1;
        }
        checkSort();
        return data[0];
    }

    @Override
    public int max() {
        if (size == 0) {
            return -1;
        }
        checkSort();
        return data[size - 1];
    }

    public boolean contains(int value) {
        if (size == 0) {
            return false;
        } else if (size == 1) {
            return data[0] == value;
        }
        checkSort();
        int first = data[0];
        if (first == value) {
            return true;
        }
        int last = data[size - 1];
        if (last == value) {
            return true;
        }
        if (value < first || value > last) {
            return false;
        }
        return indexOf(value) >= 0;
    }

    @Override
    public boolean _add(int value) {
        if (size == 0) {
            size = 1;
            data[0] = value;
            return true;
        }
        checkSort();
        int lastValue = data[size - 1];
        if (value == lastValue) {
            return false;
        } else if (value > lastValue) {
            maybeGrow();
            data[size] = value;
            size++;
            return true;
        }
        int firstValue = data[0];
        if (value == firstValue) {
            return false;
        }
        int ix = indexOf(value);
        if (ix > 0) {
            return false;
        }
        maybeGrow();
        data[size] = value;
        size++;
        sorted = false;
        return true;
    }

    private int indexOf(int value) {
        if (size == 0) {
            return -1;
        }
        checkSort();
        int result = Arrays.binarySearch(data, 0, size, value);
        return result < 0 ? -1 : result;
    }

    private void checkSort() {
        if (!sorted) {
            Arrays.sort(data, 0, size);
            sorted = true;
        }
    }

    @Override
    public boolean remove(int value) {
        if (size == 0) {
            return false;
        } else if (size == 1) {
            if (data[0] == value) {
                size--;
                return true;
            }
            return false;
        }
        int ix = indexOf(value);
        if (ix < 0) {
            return false;
        }
        System.arraycopy(data, ix + 1, data, ix, (size - ix) - 1);
        size--;
        return true;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof Integer ? contains(((Integer) o).intValue()) : false;
    }

    @Override
    public PrimitiveIterator.OfInt iterator() {
        return new PI();
    }

    @Override
    public Object[] toArray() {
        return ArrayUtils.toBoxedArray(Arrays.copyOf(data, size));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a == null || a.length < size) {
            a = CollectionUtils.genericArray((Class<T>) a.getClass().getComponentType(), size);
        }
        for (int i = 0; i < size; i++) {
            Integer val = data[i];
            a[i] = (T) val;
        }
        return a;
    }

    @Override
    public boolean add(Integer e) {
        return add(e.intValue());
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Integer)) {
            return false;
        }
        return remove(((Integer) o).intValue());
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        c = IntSetReadOnly.unwrap(c);
        if (c instanceof IntSet) {
            IntSet is = (IntSet) c;
            PrimitiveIterator.OfInt it = is.iterator();
            while (it.hasNext()) {
                if (!contains(it.nextInt())) {
                    return false;
                }
            }
            return true;
        }
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        if (c.isEmpty()) {
            return false;
        }
        c = IntSetReadOnly.unwrap(c);
        if (c instanceof IntSet) {
            c = (IntSet) IntSetReadOnly.unwrap((Collection<?>) c);
            if (c instanceof IntSetArray) {
                IntSetArray isa = (IntSetArray) c;
                if (isEmpty() || last() < isa.first()) {
                    maybeGrowToAccomodate(isa.size);
                    assert data.length > isa.size + size :
                            "Data array not resized sufficiently for "
                            + (isa.size + size) + ": " + data.length;
                    System.arraycopy(isa.data, 0, data, size, isa.size);
                    size += isa.size;
                    sorted = false;
                    return true;
                }
            }
            IntSet arr = (IntSet) c;
            IntSetArray notPresent = new IntSetArray(arr.size());
            arr.forEachInt(val -> {
                if (!contains(val)) {
                    notPresent.add(val);
                }
            });
            if (notPresent.size == 0) {
                return false;
            }
            maybeGrowToAccomodate(notPresent.size);
            assert data.length > notPresent.size + size :
                    "Data array not resized sufficiently for "
                    + (notPresent.size + size) + ": " + data.length;
            System.arraycopy(notPresent.data, 0, data, size, notPresent.size);
            size += notPresent.size;
            sorted = false;
            return true;
        }
        boolean result = false;
        for (Integer value : c) {
            result |= add(value);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        c = IntSetReadOnly.unwrap(c);
        if (c instanceof IntSet) {
            IntSet other = (IntSet) c;
            IntSet indices = new IntSetImpl();
            for (int i = 0; i < size; i++) {
                if (!other.contains(data[i])) {
                    indices.add(i);
                }
            }
            if (indices.isEmpty()) {
                return false;
            }
            removeIndices(indices);
            return true;
        }
        boolean result = false;
        assert size <= data.length : "Size is > array length";
        for (int ix = size - 1; ix >= 0; ix--) {
            int v = data[ix];
            if (!c.contains(Integer.valueOf(v))) {
                if (ix + 1 < size) {
                    int amt = (size - ix) - 1;
                    System.arraycopy(data, ix + 1, data, ix, amt);
                }
                size--;
                result = true;
                if (size == 0) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public boolean removeAll(IntSet ints) {
        if (ints == this) {
            boolean wasEmpty = size > 0;
            clear();
            return !wasEmpty;
        } else if (ints.isEmpty()) {
            return false;
        }
        IntSet indices = new IntSetImpl(ints.size());
        ints.forEachInt(val -> {
            int ix = indexOf(val);
            if (ix >= 0) {
                indices.add(ix);
            }
        });
        if (indices.isEmpty()) {
            return false;
        }
        removeIndices(indices);
        return true;
    }

    @Override
    public IntSet intersection(IntSet other) {
        IntSetArray nue = new IntSetArray(this);
        IntSet indices = new IntSetImpl(size);
        for (int i = 0; i < size; i++) {
            int val = data[i];
            if (!other.contains(val)) {
                indices.add(i);
            }
        }
        nue.removeIndices(indices);
        return nue;
    }

    @Override
    public int pick(Random r) {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException();
        }
        return data[r.nextInt(size)];
    }

    @Override
    public boolean isArrayBased() {
        return true;
    }

    @Override
    public int last() {
        if (size == 0) {
            throw new IndexOutOfBoundsException();
        }
        checkSort();
        return data[size - 1];
    }

    @Override
    public int first() {
        if (size == 0) {
            throw new IndexOutOfBoundsException();
        }
        checkSort();
        return data[0];
    }

    private void removeIndices(IntSet indices) {
        checkSort();
        indices.visitConsecutiveIndicesReversed((first, last, count) -> {
            first = indices.valueAt(first);
            last = indices.valueAt(last);
            if (last == size - 1) {
                size -= count;
            } else {
                shiftData(last + 1, first, size - (last + 1));
                size -= count;
            }
        });
    }

    void shiftData(int srcIx, int destIx, int len) {
        System.arraycopy(data, srcIx, data, destIx, len);
    }

    @Override
    public int visitConsecutiveIndices(ConsecutiveItemsVisitor v) {
        if (size == 0) {
            return 0;
        } else if (size == 1) {
            v.items(0, 0, data[0]);
            return 1;
        }
        checkSort();
        int startIx = 0;
        int count = 1;
        int prev = data[0];
        int total = 0;
        for (int i = 1; i < size; i++) {
            int curr = data[i];
            if (curr - prev == 1) {
                count++;
            } else {
                v.items(startIx, startIx + count - 1, count);
                count = 1;
                startIx = i;
                total++;
            }
            prev = curr;
            if (i == size - 1) {
                v.items(startIx, startIx + count - 1, count);
                total++;
            }
        }
        return total;
    }

    @Override
    public int visitConsecutiveIndicesReversed(ConsecutiveItemsVisitor v) {
        if (size == 0) {
            return 0;
        } else if (size == 1) {
            v.items(0, 0, 1);
            return 1;
        }
        int total = 0;
        int endIx = size - 1;
        int next = data[endIx];
        for (int i = size - 2; i >= 0; i--) {
            int curr = data[i];
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

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
            result |= remove(o);
        }
        return result;
    }

    @Override
    public void clear() {
        size = 0;
    }

    public IntSetArray addAll(int... items) {
        for (int i = 0; i < items.length; i++) {
            add(items[i]);
        }
        return this;
    }

    @Override
    BitSet bitsUnsafe() {
        return toBits();
    }

    public IntSetArray copy() {
        return new IntSetArray(this);
    }

    public IntSetArray or(IntSet other) {
        IntSetArray result = copy();
        IntSetArray notPresent = new IntSetArray(other.size());
        other.forEachInt(val -> {
            if (!contains(val)) {
                notPresent.add(val);
            }
        });
        if (!notPresent.isEmpty()) {
            maybeGrowToAccomodate(notPresent.size);
            System.arraycopy(notPresent.data, 0, result.data, size - 1,
                    notPresent.data.length);
        }
        return result;
    }

    @Override
    public IntSetArray xor(IntSet other) {
        IntSetArray result = new IntSetArray(Math.max(size, other.size()));
        for (int i = 0; i < size; i++) {
            int v = data[i];
            if (!other.contains(v)) {
                result.add(v);
            }
        }
        other.forEachInt((IntConsumer) val -> {
            if (!contains(val)) {
                result.add(val);
            }
        });
        return result;
    }

    @Override
    public BitSet toBits() {
        // XXX this may be dangerous if you have very
        // large values
        BitSet set = new BitSet(size);
        for (int i = 0; i < size; i++) {
            set.set(data[i]);
        }
        return set;
    }

    @Override
    public int removeLast() {
        int val = size == 0 ? -1 : data[size - 1];
        size = Math.max(size - 1, 0);
        return val;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void forEach(IntConsumer cons) {
        for (int i = 0; i < size; i++) {
            cons.accept(data[i]);
        }
    }

    @Override
    public void forEachReversed(IntConsumer cons) {
        for (int i = size - 1; i >= 0; i--) {
            cons.accept(data[i]);
        }
    }

    @Override
    public int[] toIntArray() {
        return size == 0 ? new int[0] : Arrays.copyOf(data, size);
    }

    @Override
    public int removeFirst() {
        if (size == 0) {
            return -1;
        }
        int result = data[0];
        System.arraycopy(data, 1, data, 0, size - 1);
        return result;
    }

    @Override
    public int hashCode() {
        //follows the contract of AbstractSet.hashCode()
        int h = 0;
        for (int i = 0; i < size; i++) {
            h += data[i];
        }
        return h;
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        }
        if (o instanceof IntSetReadOnly) {
            o = IntSetReadOnly.unwrap((Collection<?>) o);
        }
        if (o instanceof IntSetArray) {
            IntSetArray isa = (IntSetArray) o;
            if (isa.size != size) {
                return false;
            }
            checkSort();
            isa.checkSort();
            // PENDING: JDK 9's Arrays.equals has subranges
            for (int i = 0; i < size; i++) {
                if (data[i] != isa.data[i]) {
                    return false;
                }
            }
            return true;
        } else if (o instanceof IntSet) {
            IntSet is = (IntSet) o;
            if (is.size() != size) {
                return false;
            }
            return containsAll(is);
        } else if (o instanceof Iterable) {
            Iterable<?> it = (Iterable<?>) o;
            int count = 0;
            for (Object elem : it) {
                if (count++ > size) {
                    return false;
                }
                if (!contains(elem)) {
                    return false;
                }
            }
            return count == size;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(size() * 8).append('[');
        for (int i = 0; i < size; i++) {
            int curr = data[i];
            sb.append(curr);
            if (i != size - 1) {
                sb.append(", ");
            }
        }
        return sb.append(']').toString();
    }

    class PI implements PrimitiveIterator.OfInt {

        private int cursor = -1;

        @Override
        public int nextInt() {
            return data[++cursor];
        }

        @Override
        public boolean hasNext() {
            return cursor + 1 < size;
        }

        @Override
        public void remove() {
            if (cursor < 0) {
                throw new IndexOutOfBoundsException();
            }
            System.arraycopy(data, cursor + 1, data, cursor, size - (cursor + 1));
            size--;
        }
    }
}
