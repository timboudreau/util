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

import com.mastfrog.util.sort.Sort;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 *
 * @author Tim Boudreau
 */
final class IntIntMapImpl implements IntIntMap, Iterable<Integer> {

    private final int initialCapacity;
    private boolean sorted;
    private int[] keys;
    private int[] values;
    private int size = 0;

    IntIntMapImpl() {
        this(16);
    }

    IntIntMapImpl(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Negative capacity " + initialCapacity);
        }
        this.initialCapacity = initialCapacity;
        keys = new int[initialCapacity];
        values = new int[initialCapacity];
        sorted = true;
    }

    IntIntMapImpl(int[] keys, int[] values, boolean checkDuplicates, boolean sorted) {
        if (keys.length != values.length) {
            throw new IllegalArgumentException("Keys and values arrays do "
                    + "not have the same length: " + keys.length + " and "
                    + values.length);
        }
        if (checkDuplicates && keys.length > 0) {
            int[] test = Arrays.copyOf(keys, keys.length);
            Arrays.sort(test);
            int last = test[0];
            for (int i = 1; i < test.length; i++) {
                int curr = test[i];
                if (curr == last) {
                    throw new IllegalArgumentException("Keys array contains "
                            + "duplicates: " + curr);
                }
                last = curr;
            }
        }
        this.keys = Arrays.copyOf(keys, keys.length);
        this.values = Arrays.copyOf(values, values.length);
        this.sorted = sorted;
        initialCapacity = Math.max(16, keys.length);
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (int i = 0; i < size; i++) {
            result += keys[i];
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof IntIntMapImpl) {
            IntIntMapImpl other = (IntIntMapImpl) o;
            if (other.size == size) {
                if (isEmpty()) {
                    return true;
                }
                if (other.greatestKey() == greatestKey() && other.leastKey() == leastKey()) {
                    for (int i = 1; i < size - 2; i++) {
                        if (keys[i] != other.keys[i]) {
                            return false;
                        }
                        if (values[i] != other.values[i]) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        } else if (o instanceof Map<?, ?>) {
            Map<?, ?> m = (Map<?, ?>) o;
            if (isEmpty() && m.isEmpty()) {
                return true;
            }
            for (Entry<?, ?> e : m.entrySet()) {
                Object k = e.getKey();
                Object v = e.getValue();
                if (!(k instanceof Integer) || !(v instanceof Integer)) {
                    return false;
                }
                int ki = ((Integer) k).intValue();
                int ix = indexOf(ki);
                if (ix < 0) {
                    return false;
                }
                int vi = ((Integer) v).intValue();
                if (values[ix] != vi) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int valueAt(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index out of bounds "
                    + 0 + "-" + size + ": " + index);
        }
        return values[index];
    }

    @Override
    public int setValueAt(int index, int value) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index out of bounds "
                    + 0 + "-" + size + ": " + index);
        }
        int old = values[index];
        values[index] = value;
        return old;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(size * 5);
        for (int i = 0; i < size; i++) {
            sb.append(keys[i]).append('=').append(values[i]);
            if (i != size - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    @Override
    public PrimitiveIterator.OfInt iterator() {
        return new PrimitiveIterator.OfInt() {
            private int cursor = -1;

            @Override
            public int nextInt() {
                return keys[++cursor];
            }

            @Override
            public boolean hasNext() {
                return cursor + 1 < size;
            }
        };
    }

    @Override
    public IntSet keySet() {
        return new IntSetArray(keys, size, sorted);
    }

    @Override
    public void put(int key, int value) {
        int oldIx = indexOf(key);
        if (oldIx < 0) {
            sorted = size == 0 ? false
                    : key > greatestKey();
            maybeGrow();
            keys[size] = key;
            values[size] = value;
            size++;
        } else {
            values[oldIx] = value;
        }
    }

    @Override
    public void clear() {
        size = 0;
    }

    public int greatestKey() {
        if (size == 0) {
            throw new IndexOutOfBoundsException("Empty");
        }
        checkSort();
        return keys[size - 1];
    }

    public int leastKey() {
        if (size == 0) {
            throw new IndexOutOfBoundsException("Empty");
        }
        checkSort();
        return keys[0];
    }

    private void checkSort() {
        if (!sorted) {
            Sort.multiSort(keys, 0, size, (ixA, ixB) -> {
                int hold = values[ixA];
                values[ixA] = values[ixB];
                values[ixB] = hold;
            });
        }
    }

    public int indexOf(int key) {
        checkSort();
        return Arrays.binarySearch(keys, 0, size, key);
    }

    private void maybeGrow() {
        if (size + 1 == keys.length) {
            int growBy = Math.max(initialCapacity, keys.length / 2);
            int newSize = keys.length + growBy;
            keys = Arrays.copyOf(keys, newSize);
            values = Arrays.copyOf(values, newSize);
        }
    }

    @Override
    public boolean remove(int key) {
        int ix = indexOf(key);
        if (ix < 0) {
            return false;
        }
        removeIndex(ix);
        return true;
    }

    @Override
    public int removeAll(IntSet keys) {
        if (keys.isEmpty()) {
            return 0;
        }
        IntSet indices = new IntSetImpl();
        keys.forEachInt(key -> {
            int ix = indexOf(key);
            if (ix >= 0) {
                indices.add(ix);
            }
        });
        removeIndices(indices);
        return indices.size();
    }

    @Override
    public int getAsInt(int key) {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException();
        }
        int ix = indexOf(key);
        if (ix < 0) {
            throw new NoSuchElementException();
        }
        return values[ix];
    }

    @Override
    public int getAsInt(int key, int defaultValue) {
        if (size == 0) {
            return defaultValue;
        }
        int ix = indexOf(key);
        if (ix < 0) {
            return defaultValue;
        }
        return values[ix];
    }

    @Override
    public void forEachKey(IntConsumer c) {
        for (int i = 0; i < size; i++) {
            c.accept(keys[i]);
        }
    }

    @Override
    public void forEachValue(IntConsumer c) {
        for (int i = 0; i < size; i++) {
            c.accept(values[i]);
        }
    }

    @Override
    public void forEachPair(IntIntMapConsumer c) {
        for (int i = 0; i < size; i++) {
            c.item(keys[i], values[i]);
        }
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
    public boolean containsKey(Object key) {
        if (!(key instanceof Integer)) {
            return false;
        }
        return indexOf(((Integer) key).intValue()) >= 0;
    }

    @Override
    public boolean containsValue(Object value) {
        if (isEmpty()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (Integer.valueOf(values[i]).equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Integer get(Object key) {
        if (!(key instanceof Integer)) {
            return null;
        }
        int intKey = ((Integer) key).intValue();
        return getAsInt(intKey);
    }

    @Override
    public Integer getOrDefault(Object key, Integer defaultValue) {
        if (!(key instanceof Integer)) {
            return defaultValue;
        }
        int intKey = ((Integer) key).intValue();
        int ix = indexOf(intKey);
        if (ix < 0) {
            return defaultValue;
        }
        return values[ix];
    }

    @Override
    public Integer put(Integer key, Integer value) {
        Integer result = getOrDefault(key, null);
        put(key.intValue(), value.intValue());
        return result;
    }

    public int removeIndices(IntSet indices) {
        if (indices.isEmpty()) {
            return 0;
        }
        int start = -1;
        int len = 0;
        int[] all = indices.toIntArray();
        for (int i = all.length - 1; i >= 0; i--) {
            int val = all[i];
            if (start != -1) {
                if (val == start - 1) {
                    start = val;
                    len++;
                } else {
                    removeConsecutiveIndices(start, len);
                    start = val;
                    len = 1;
                }
            } else {
                start = val;
                len = 1;
            }
        }
        if (start != -1) {
            removeConsecutiveIndices(start, len);
        }
        return indices.size();
    }

    public int removeIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index + " out of range 0-" + index);
        }
        return removeIndexImpl(index);
    }

    @SuppressWarnings("unchecked")
    private int removeIndexImpl(int index) {
        int result = values[index];
        if (index == 0) {
            if (size == 1) {
                size = 0;
                this.sorted = true;
                return result;
            }
            System.arraycopy(keys, 1, keys, 0, size - 1);
            System.arraycopy(values, 1, values, 0, size - 1);
            size--;
        } else if (index == size - 1) {
            keys[size - 1] = Integer.MAX_VALUE;
            size--;
            return result;
        } else {
            System.arraycopy(keys, index + 1, keys, index, keys.length - (index + 1));
            System.arraycopy(values, index + 1, values, index, values.length - (index + 1));
            size--;
        }
        return result;
    }

    public void removeConsecutiveIndices(int start, int length) {
        if (length == 1) {
            removeIndex(start);
            return;
        } else if (length < 1) {
            return;
        } else if (start < 0 || start >= size() || start + length > size()) {
            throw new IndexOutOfBoundsException("Attempt to remove range "
                    + start + ":" + (start + length) + " of " + size());
        }
        shiftData(start + length, start, size() - (start + length));
        size -= length;
    }

    void shiftData(int srcIx, int destIx, int len) {
        System.arraycopy(keys, srcIx, keys, destIx, len);
        System.arraycopy(values, srcIx, values, destIx, len);
    }

    @Override
    public Integer remove(Object key) {
        if (key instanceof Integer) {
            int ix = indexOf((Integer) key);
            if (ix < 0) {
                return null;
            }
            return removeIndex(ix);
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends Integer> m) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Integer> values() {
        return new IntListImpl(valuesArray());
    }

    @Override
    public Set<Entry<Integer, Integer>> entrySet() {
        Set<Map.Entry<Integer, Integer>> result = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) {
            result.add(new IntEntry(i));
        }
        return result;
    }

    public int[] keysArray() {
        return Arrays.copyOf(keys, size);
    }

    public int[] valuesArray() {
        return Arrays.copyOf(values, size);
    }

    @Override
    public int key(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " out of "
                    + "range 0-" + size);
        }
        return keys[index];
    }

    @Override
    public PrimitiveIterator.OfInt keysIterator() {
        return new PrimitiveIterator.OfInt() {
            private int ix = -1;

            @Override
            public int nextInt() {
                return keys[++ix];
            }

            @Override
            public boolean hasNext() {
                return ix + 1 < size;
            }
        };
    }

    @Override
    public int nearestKey(int key, boolean backward) {
        int last = size - 1;
        checkSort();
        if (isEmpty()) {
            return -1;
        }
        if (last == 0) {
            return keys[last];
        }
        if (key < keys[0]) {
            return backward ? keys[last] : keys[0];
        }
        if (key > keys[last]) {
            return backward ? keys[last] : keys[0];
        }
        int idx = Arrays.binarySearch(keys, 0, last + 1, key);
        if (idx < 0) {
            idx = -idx + (backward ? -2 : - 1);
            if (idx > last) {
                idx = backward ? last : 0;
            } else if (idx < 0) {
                idx = backward ? last : 0;
            }
        }
        return keys[idx];
    }

    public int nearestIndexTo(int key, boolean backward) {
        int last = size - 1;
        if (key < keys[0]) {
            return backward ? last : 0;
        }
        if (key > keys[last]) {
            return backward ? last : 0;
        }
        int idx = Arrays.binarySearch(keys, 0, last + 1, key);
        if (idx < 0) {
            idx = -idx + (backward ? -2 : - 1);
            if (idx > last) {
                idx = backward ? last : 0;
            } else if (idx < 0) {
                idx = backward ? last : 0;
            }
        }
        return idx;
    }

    @Override
    public boolean containsKey(int key) {
        return indexOf(key) >= 0;
    }

    class IntEntry implements Map.Entry<Integer, Integer> {

        private final int index;

        public IntEntry(int index) {
            this.index = index;
        }

        @Override
        public Integer getKey() {
            return keys[index];
        }

        @Override
        public Integer getValue() {
            return values[index];
        }

        @Override
        public Integer setValue(Integer value) {
            Integer old = getKey();
            values[index] = value;
            return old;
        }
    }

}
