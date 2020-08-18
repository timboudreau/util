/*
 * The MIT License
 *
 * Copyright 2004 Tim Boudreau.
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

import com.mastfrog.util.collections.CollectionUtils.ComparableComparator;
import com.mastfrog.util.sort.Sort;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Sparse array integer keyed map. Similar to a standard Collections map, but
 * considerably more efficient for this purpose, it simply an array of integer
 * indices that have values and an array of objects mapped to those indices.
 * Entries may be added only in ascending order, enabling use of
 * Arrays.binarySearch() to quickly locate the relevant entry.
 * <p>
 * This class was originally written for NetBeans output window in 2004.
 * </p>
 *
 * @author Tim Boudreau
 */
final class ArrayIntMap<T> implements IntMap<T> {

    private static final boolean DEBUG = Boolean.getBoolean("ArrayIntMap.debug");
    int[] keys;
    private Object[] vals;
    private int last = -1;
    private final Supplier<T> emptyValue;
    int nextKey = 0;
    boolean resort;
    private boolean addSuppliedValues;

    private ArrayIntMap(ArrayIntMap<T> other) {
        keys = Arrays.copyOf(other.keys, other.keys.length);
        vals = Arrays.copyOf(other.vals, other.vals.length);
        last = other.last;
        emptyValue = other.emptyValue;
        resort = other.resort;
        nextKey = other.nextKey;
    }

    /**
     * Creates a new instance of ArrayIntMap
     */
    ArrayIntMap() {
        keys = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE,
            Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        vals = new Object[5];
        emptyValue = null;
    }

    ArrayIntMap(int minCapacity) {
        this(minCapacity, true, null);
    }

    ArrayIntMap(int minCapacity, boolean addSuppliedValues, Supplier<T> emptyValue) {
        if (minCapacity <= 0) {
            throw new IllegalArgumentException("Must be > 0");
        }
        this.addSuppliedValues = addSuppliedValues;
        this.emptyValue = emptyValue;
        keys = new int[minCapacity];
        vals = new Object[minCapacity];
        Arrays.fill(keys, Integer.MAX_VALUE);
    }

    ArrayIntMap(Map<Integer, T> from) {
        this.emptyValue = null;
        int max = from.size();
        keys = new int[max];
        vals = new Object[max];
        int index = 0;
        int lastKey = Integer.MIN_VALUE;
        for (Map.Entry<Integer, T> en : from.entrySet()) {
            int key = en.getKey();
            keys[index] = key;
            vals[index++] = en.getValue();
            resort |= key <= lastKey;
            lastKey = key;
        }
        nextKey = max;
        last = max - 1;

//        if (resort) {
//            checkSort();
//        }
    }

    ArrayIntMap(int[] keys, Object[] vals) {
        if (keys.length != vals.length) {
            throw new IllegalArgumentException("Key and value array sizes do "
                    + "not match: " + keys.length + " and " + vals.length);
        }
        if (keys.length > 0) {
            int last = -1;
            resort = false;
            int max = last;
            for (int i = 0; i < keys.length; i++) {
                int val = keys[i];
                if (val == last) {
                    throw new IllegalArgumentException("Duplicate keys: " + val);
                }
                if (val < last) {
                    resort = true;
                }
                last = val;
                max = Math.max(max, last);
            }
            nextKey = max + 1;
            this.keys = Arrays.copyOf(keys, keys.length);
            this.vals = Arrays.copyOf(vals, vals.length);
            this.last = this.vals.length - 1;
            emptyValue = null;
        } else {
            last = -1;
            this.keys = new int[5];
            this.vals = new Object[5];
            nextKey = Integer.MIN_VALUE;
            emptyValue = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T valueAt(int index) {
        checkSort();
        if (index < 0 || index > last) {
            throw new IndexOutOfBoundsException("Index " + index
                    + " out of range 0-" + last);
        }
        return (T) vals[index];
    }

    @SuppressWarnings("unchecked")
    @Override
    public T leastValue() {
        checkSort();
        if (last >= 0) {
            return (T) vals[0];
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T greatestValue() {
        checkSort();
        if (last >= 0) {
            return (T) vals[last];
        }
        return null;
    }

    @Override
    public int indexOf(int key) {
        checkSort();
        if (last < 0) {
            return -1;
        } else if (key == keys[0]) {
            return 0;
        } else if (key < keys[0]) {
            return -1;
        } else if (key == keys[last]) {
            return last;
        } else if (key > keys[last]){
            return -1;
        }
        return Arrays.binarySearch(keys, 0, last + 1, key);
    }

    boolean consistent() {
        if (!DEBUG) {
            return true;
        }
        if (last >= 0) {
            if (last > 1) {
                if (!resort) {
                    int prev = keys[0];
                    for (int i = 1; i <= last; i++) {
                        int curr = keys[i];
                        if (curr <= prev) {
                            throw new AssertionError("Keys state inconsistent at "
                                    + i + " with " + prev + " and " + curr
                                    + " in " + Arrays.toString(Arrays.copyOf(keys, last + 1)));
                        }
                        prev = curr;
                    }
                }
                int[] snapshot = Arrays.copyOf(keys, last + 1);
                Arrays.sort(snapshot);
                int prev = snapshot[0];
                for (int i = 1; i < snapshot.length; i++) {
                    if (prev == snapshot[i]) {
                        throw new AssertionError("Keys contain duplicates at "
                                + i + " with " + prev + " and " + snapshot[i]
                                + " in " + Arrays.toString(snapshot));
                    }
                    prev = snapshot[i];
                }
            }
            if (!resort) {
                assert nextKey == keys[last] + 1 : "nextKey inconsistent "
                        + nextKey + " should be " + (keys[last] + 1)
                        + " in " + Arrays.toString(Arrays.copyOf(keys, last + 1));
            } else {
                int max = Integer.MIN_VALUE;
                for (int i = 0; i <= last; i++) {
                    max = Math.max(keys[i], max);
                }
                assert nextKey == max + 1 : "nextKey inconsistent "
                        + nextKey + " should be " + (max + 1)
                        + " in " + Arrays.toString(Arrays.copyOf(keys, last + 1));
            }
        }
        return true;
    }

    // A few methods for tests
    int[] keysUnsafe() {
        return keys;
    }

    Object[] valuesUnsafe() {
        return vals;
    }

    void setKeyUnsafe(int index, int key) {
        keys[index] = key;
    }

    void setResortUnsafe(boolean val) {
        resort = val;
    }

    int nextKeyUnsafe() {
        return nextKey;
    }

    @Override
    public void trim() {
        if (last < 0) {
            keys = new int[3];
            vals = new Object[3];
        } else {
            int newSize = Math.max(last + 1, 3);
            keys = Arrays.copyOf(keys, newSize);
            vals = Arrays.copyOf(vals, newSize);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int valuesBetween(int first, int second, IntMapConsumer<T> c) {
        int v1 = Math.min(first, second);
        int v2 = Math.max(first, second);
        if (last < 0) {
            return 0;
        } else if (last == 0) {
            if (keys[0] <= v2 && keys[0] >= v1) {
                c.accept(keys[0], (T) vals[0]);
                return 1;
            }
            return 0;
        }
        checkSort();
        v1 = Math.max(keys[0], v1);
        v2 = Math.min(keys[last], v2);
        int ix1 = nearestIndexTo(v1, false);
        int ix2 = nearestIndexTo(v2, true);
        if (ix1 == ix2) {
            int v = keys[ix1];
            if (v <= v2 && v >= v1) {
                c.accept(v, (T) vals[ix1]);
                return 1;
            }
            return 0;
        } else {
            int count = 0;
            for (int i = Math.min(ix1, ix2); i <= Math.max(ix1, ix2); i++) {
                if (keys[i] >= v1 && keys[i] <= v2) {
                    c.accept(keys[i], (T) vals[i]);
                    count++;
                }
            }
            return count;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int keysAndValuesBetween(int first, int second, IndexedIntMapConsumer<T> c) {
        int v1 = Math.min(first, second);
        int v2 = Math.max(first, second);
        if (last < 0) {
            return 0;
        } else if (last == 0) {
            if (keys[0] <= v2 && keys[0] >= v1) {
                c.accept(0, keys[0], (T) vals[0]);
                return 1;
            }
            return 0;
        }
        checkSort();
        v1 = Math.max(keys[0], v1);
        v2 = Math.min(keys[last], v2);
        int ix1 = nearestIndexTo(v1, false);
        int ix2 = nearestIndexTo(v2, true);
        assert consistent();
        if (ix1 == ix2) {
            int v = keys[ix1];
            if (v <= v2 && v >= v1) {
                c.accept(ix1, v, (T) vals[ix1]);
                return 1;
            }
            return 0;
        } else {
            int count = 0;
            for (int i = Math.min(ix2, ix1); i <= Math.max(ix2, ix1); i++) {
                if (keys[i] >= v1 && keys[i] <= v2) {
                    count++;
                    c.accept(i, keys[i], (T) vals[i]);
                }
            }
            return count;
        }
    }

    @Override
    public void setValueAt(int index, T obj) {
        if (index < 0 || index > last) {
            throw new IllegalArgumentException("Index " + index
                    + " out of range 0-" + last);
        }
        vals[index] = obj;
    }

    @Override
    public T removeIndex(int index) {
        if (index < 0 || index > last) {
            throw new IndexOutOfBoundsException(index + " out of range 0-" + index);
        }
        return removeIndexImpl(index);
    }

    @Override
    public int[] keysArray() {
        if (last == -1) {
            return new int[0];
        }
        checkSort();
        return Arrays.copyOf(keys, last + 1);
    }

    @Override
    public Object[] valuesArray() {
        return Arrays.copyOf(vals, last + 1);
    }

    public ArrayIntMap<T> copy() {
        return new ArrayIntMap<>(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean forSomeKeys(IntMapAbortableConsumer<? super T> cons) {
        for (int i = 0; i < size(); i++) {
            boolean result = cons.accept(keys[i], (T) this.vals[i]);
            if (!result) {
                return false;
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public void forEach(IntMapConsumer<? super T> cons) {
        for (int i = 0; i < size(); i++) {
            cons.accept(keys[i], (T) this.vals[i]);
        }
    }

    @Override
    public int removeIndices(IntSet indices) {
        if (indices.isEmpty()) {
            return 0;
        }
        int start = -1;
        int len = 0;
        if (indices instanceof IntSetArray) {
            IntSetArray arr = (IntSetArray) indices;
            arr.checkSort();
        } else if ((indices instanceof IntSetReadOnly && ((IntSetReadOnly) indices).delegate instanceof IntSetArray)) {
            IntSetArray arr = (IntSetArray) ((IntSetReadOnly) indices).delegate;
            arr.checkSort();
        }
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
        if (last < 0) {
            nextKey = 0;
        } else {
            nextKey = keys[last] + 1;
        }
        return indices.size();
    }

    @Override
    public int key(int index) {
        if (index < 0 || index > last + 1) {
            throw new IndexOutOfBoundsException("Index " + index
                    + " out of range 0-" + last);
        }
        checkSort();
        return keys[index];
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
        last -= length;
    }

    void shiftData(int srcIx, int destIx, int len) {
        System.arraycopy(keys, srcIx, keys, destIx, len);
        System.arraycopy(vals, srcIx, vals, destIx, len);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void forEachIndexed(IndexedIntMapConsumer<? super T> cons) {
        checkSort();
        for (int i = 0; i < size(); i++) {
            cons.accept(i, keys[i], (T) this.vals[i]);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void forEachReversed(IndexedIntMapConsumer<? super T> cons) {
        checkSort();
        for (int i = size() - 1; i >= 0; i--) {
            cons.accept(i, keys[i], (T) this.vals[i]);
        }
    }

    public int[] keys() {
        checkSort();
        int[] result = new int[size()];
        System.arraycopy(keys, 0, result, 0, result.length);
        return result;
    }

    @Override
    public int leastKey() {
        checkSort();
        return isEmpty() ? -1 : keys[0];
    }

    @Override
    public int greatestKey() {
        checkSort();
        return isEmpty() ? -1 : keys[last];
    }

    int keyForIndex(int ix) {
        return keys[ix];
    }

    @SuppressWarnings("unchecked")
    T forIndex(int ix) {
        return (T) vals[ix];
    }

    @Override
    public boolean containsKey(int key) {
        if (key >= nextKey) {
            return false;
        }
        if (isEmpty()) {
            return false;
        } else if (last == 0) {
            return key == keys[0];
        } else if (resort) {
            int max = size();
            for (int i = 0; i < max; i++) {
                if (keys[i] == key) {
                    return true;
                }
            }
            return false;
        } else {
            if (key == keys[0] || key == keys[last]) {
                return true;
            } else if (resort) {
                checkSort();
                if (key == keys[0] || key == keys[last]) {
                    return true;
                }
            }
            if (this.emptyValue == null) {
                assert !resort;
                if (key < keys[0] || key > keys[last]) {
                    return false;
                }
//                assert noDuplicates();
                int ix = Arrays.binarySearch(keys, 0, last + 1, key);
                return ix >= 0;
            } else {
                return this.getIfPresent(key, null) != null;
            }
        }
    }

    @Override
    public int nearestKey(int key, boolean backward) {
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

    @Override
    public int nearestIndexTo(int key, boolean backward) {
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

    /**
     * Some temporary diagnostics re issue 48608
     */
    private static String i2s(int[] arr) {
        StringBuilder sb = new StringBuilder((arr.length * 3) + 2);
        sb.append('[');
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != Integer.MAX_VALUE) {
                sb.append(arr[i]);
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getIfPresent(int key, T defaultValue) {
        if (last < 0) {
            return defaultValue;
        } else if (last == 0) {
            if (key == keys[0]) {
                return (T) vals[0];
            } else {
                return defaultValue;
            }
        }
        checkSort();
        int idx = Arrays.binarySearch(keys, 0, last + 1, key);
        T result = null;
        if (idx > -1 && idx <= last) {
            result = (T) vals[idx];
        } else {
            result = defaultValue;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(int key) {
        if (last < 0) {
            T result = null;
            if (emptyValue != null) {
                result = emptyValue.get();
                if (result != null && addSuppliedValues) {
                    last = 0;
                    keys[0] = key;
                    vals[0] = result;
                    nextKey = Math.max(nextKey, key + 1);
                }
            }
            return result;
        } else if (last == 0) {
            if (key == keys[0]) {
                return (T) vals[0];
            } else if (emptyValue != null) {
                T result = emptyValue.get();
                if (result != null && addSuppliedValues) {
                    put(key, result);
                }
                return result;
            }
            return null;
        }
        checkSort();
        int idx = Arrays.binarySearch(keys, 0, last + 1, key);
        T result = null;
        if (idx > -1 && idx <= last) {
            result = (T) vals[idx];
        }
        if (result == null && emptyValue != null && addSuppliedValues) {
            result = emptyValue.get();
            if (result != null) {
                put(key, result);
            }
            return result;
        }
        return result == null ? emptyValue == null
                ? null : emptyValue.get() : result;
    }

    public int removeAll(IntSet keys) {
        IntSet contained = keys.intersection(keySet());
        IntSet indices = new IntSetImpl(contained.size());
        contained.forEachInt(val -> {
            int ix = indexOf(val);
            if (ix >= 0) {
                indices.add(ix);
            }
        });
        if (indices.isEmpty()) {
            return 0;
        }
        removeIndices(indices);
        return indices.size();
    }

    public void putAll(IntMap<T> map) {
        if (map.isEmpty()) {
            return;
        }
        int currentSize = size();
        int ms = map.size();
        checkSort();
        if (currentSize == 0 || map.key(ms - 1) >= keys[last]) {
            if (map instanceof ArrayIntMap<?>) {
                ((ArrayIntMap<?>) map).checkSort();
                assert ((ArrayIntMap<?>) map).consistent();
            }
            growFor(currentSize + ms);
            System.arraycopy(map.keysArray(), 0, keys, currentSize, ms);
            System.arraycopy(map.valuesArray(), 0, vals, currentSize, ms);
            nextKey = keys[currentSize + ms - 1] + 1;
            last += ms;
            resort = true;
        } else {
            if (map instanceof ArrayIntMap<?>) {
                ((ArrayIntMap<?>) map).checkSort();
            }
            IntSet inboundKeys = map.keySet();
            IntSet isect = inboundKeys.intersection(keySet());
            IntSet newItems = inboundKeys.copy();
            newItems.removeAll(isect);

            if (!isect.isEmpty()) {
                PrimitiveIterator.OfInt it = isect.iterator();
                while (it.hasNext()) {
                    int key = it.next();
                    int ix = indexOf(key);
                    vals[ix] = map.get(key);
                }
            }
            if (!newItems.isEmpty()) {
                int sz = newItems.size();
                growFor(sz);
                System.arraycopy(newItems.toIntArray(), 0, keys, currentSize, sz);
                PrimitiveIterator.OfInt it = newItems.iterator();
                int cursor = currentSize;
                while (it.hasNext()) {
                    int next = it.next();
                    vals[cursor++] = map.get(next);
                    nextKey = Math.max(nextKey, next + 1);
                }
                last += sz;
                resort = true;
                checkSort();
                // XXX could detect if the least key inbound is
                // greater than the greatest key current, and if so,
                // avoid re-sorting, but we will trigger a sort on the
                // inbound collection, which, if larger, will be
                // equally expensive
            }
        }
        assert consistent();
    }

    static boolean isSorted(IntMap<?> map) {
        if (map instanceof ArrayIntMap<?>) {
            return !((ArrayIntMap<?>) map).resort;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T put(int key, T val) {
        if (key < nextKey) {
            // XXX find a cleaner way to do this
            checkSort();
        }
        try {
//            notNull("val", val);
            boolean between = false;
            if (last >= 0) {
                if (keys[last] == key && vals[last] == val) {
                    return val;
                } else if (keys[0] == key && vals[last] == val) {
                    return val;
                } else if (keys[last] == key) {
                    T old = (T) vals[last];
                    vals[last] = val;
                    return old;
                } else if (keys[0] == key) {
                    T old = (T) vals[0];
                    vals[0] = val;
                    return old;
                }
                if (key < keys[last]) {
                    resort = true;
                    between = key >= keys[0];
                } else if (key == keys[last]) {
                    between = true;
                }
            } else {
                last = 0;
                keys[0] = key;
                vals[0] = val;
                nextKey = key + 1;
                resort = false;
                return null;
            }
            if (between) {
                int existingIndex = Arrays.binarySearch(keys, 0, last + 1, key);
                if (existingIndex >= 0) {
                    T old = (T) vals[existingIndex];
                    vals[existingIndex] = val;
                    return old;
                }
            }
            if (last == keys.length - 1) {
                growArrays();
            }
            last++;
            nextKey = Math.max(key + 1, nextKey);
            keys[last] = key;
            vals[last] = val;
            return null;
        } finally {
            assert consistent();
        }
    }

    void checkSort() {
        if (resort) {
            sort();
            resort = false;
            if (last >= 0) {
                nextKey = keys[last] + 1;
            }
        } else {
            assert consistent();
        }
    }

    private void growArrays() {
        int newSize = last < 250 ? last + Math.max(5, last + (last / 3)) : last + (last / 2);
        keys = Arrays.copyOf(keys, newSize);
        vals = Arrays.copyOf(vals, newSize);
    }

    private void growFor(int toAdd) {
        int base = last < 250 ? last + Math.max(5, last + (last / 3)) : last + (last / 2);
        int newSize = last + 1 + Math.max(base, toAdd);
        keys = Arrays.copyOf(keys, newSize);
        vals = Arrays.copyOf(vals, newSize);
    }

    private static final int serialVersionUID = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        int[] ints = keys.length == size() ? keys : Arrays.copyOf(keys, size());
        Object[] vals = this.vals.length == size() ? this.vals : Arrays.copyOf(this.vals, size());
        out.writeObject(ints);
        out.writeObject(vals);
        out.writeBoolean(resort);
        out.writeInt(nextKey);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int[] keys = (int[]) in.readObject();
        Object[] vals = (Object[]) in.readObject();
        resort = in.readBoolean();
        nextKey = in.readInt();
        last = keys.length - 1;
        if (keys.length != vals.length) {
            throw new IOException("Different lengths arrays");
        }
        this.keys = keys;
        this.vals = vals;
    }

    /**
     * Get the key which follows the passed key, or -1. Will wrap around 0.
     */
    public int nextEntry(int entry) {
        if (last < 0) {
            return -1;
        } else {
            checkSort();
            if (entry > keys[last]) {
                return -1;
            }
        }
        checkSort();
        int result = -1;
        int idx = Arrays.binarySearch(keys, 0, last + 1, entry);
        if (idx >= 0) {
            result = idx == keys.length - 1 ? keys[0] : keys[idx + 1];
        }
        return result;
    }

    /**
     * Get the key which precedes the passed key, or -1. Will wrap around 0.
     */
    public int prevEntry(int entry) {
        if (last < 0) {
            return -1;
        } else if (entry < keys[0]) {
            return -1;
        }
        checkSort();
        int result = -1;
        if (!isEmpty()) {
            int idx = Arrays.binarySearch(keys, 0, last + 1, entry);
            if (idx >= 0) {
                result = idx == 0 - 1 ? keys[keys.length - 1] : keys[idx - 1];
            }
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return last == -1;
    }

    @Override
    public int size() {
        return last + 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(size() * 8).append('{'); //NOI18N
        int sz = size();
        for (int i = 0; i < sz; i++) {
            sb.append(keys[i]);
            sb.append("="); //NOI18N
            sb.append(vals[i]);
            if (i != sz - 1) {
                sb.append(", ");
            }
        }
        return sb.append('}').toString();
    }

    /**
     * Decrement keys in the map. Entries with negative keys will be removed.
     *
     * @param decrement Value the keys should be decremented by. Must be zero or
     * higher.
     */
    @Override
    public void decrementKeys(int decrement) {
        if (last < 0) {
            return;
        }
        checkSort();

        if (decrement < 0) {
            throw new IllegalArgumentException();
        }

        int shift = Arrays.binarySearch(keys, 0, last + 1, decrement);
        if (shift < 0) {
            shift = -shift - 1;
        }

        for (int i = shift; i <= last; i++) {
            keys[i - shift] = keys[i] - decrement;
            vals[i - shift] = vals[i];
        }
        last -= shift;
        if (last >= 0) {
            nextKey = keys[last] + 1;
        } else {
            nextKey = 0;
            last = -1;
        }
    }

    @Override
    public Iterable<Map.Entry<Integer, T>> entries() {
        return this;
    }

    @Override
    public Iterator<Map.Entry<Integer, T>> iterator() {
        if (isEmpty()) {
            return Collections.emptyIterator();
        }
        checkSort();
        return new Iter();
    }

    public Iterable<T> valuesIterable() {
        if (isEmpty()) {
            return Collections.emptyList();
        }
        checkSort();
        return () -> new ArrIter();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof Integer) {
            int keyVal = ((Integer) key).intValue();
            return this.containsKey(keyVal);
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (int i = 0; i <= last; i++) {
            if (Objects.equals(value, vals[i])) {
                return true;
            }
        }
        return false;
    }

    int keyForValue(Object value) {
        for (int i = 0; i <= last; i++) {
            if (Objects.equals(value, vals[i])) {
                return keys[i];
            }
        }
        return -1;
    }

    @Override
    public T get(Object key) {
        if (key instanceof Integer) {
            return this.get(((Integer) key).intValue());
        }
        return null;
    }

    @Override
    public T put(Integer key, T value) {
        return this.put(key.intValue(), value);
    }

    @Override
    public void forEachValue(Consumer<T> valueConsumer) {
        for (int i = 0; i < last + 1; i++) {
            valueConsumer.accept(valueAt(i));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T remove(int keyVal) {
        if (last >= 0 && keyVal == keys[last]) {
            T old = (T) vals[last];
            vals[last] = null;
            last--;
            if (last > 0) {
                nextKey = keys[last] + 1;
            } else {
                nextKey = 0;
            }
            return old;
        }
        int index = Arrays.binarySearch(keys, 0, last + 1, keyVal);
        if (index >= 0) {
            return removeIndexImpl(index);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private T removeIndexImpl(int index) {
        T result = (T) vals[index];
        if (index == 0) {
            if (last == 0) {
                last = -1;
                nextKey = 0;
                resort = false;
                return (T) vals[0];
            }
            System.arraycopy(keys, 1, keys, 0, last);
            System.arraycopy(vals, 1, vals, 0, last);
            last--;
        } else if (index == last) {
            keys[last] = Integer.MAX_VALUE;
            vals[last] = null;
            last--;
            if (last >= 0) {
                nextKey = keys[last] + 1;
            } else {
                nextKey = 0;
            }
            return result;
        } else {
            System.arraycopy(keys, index + 1, keys, index, keys.length - (index + 1));
            System.arraycopy(vals, index + 1, vals, index, vals.length - (index + 1));
            last--;
        }
        return result;
    }

    @Override
    public T remove(Object key) {
        if (last == -1) {
            return null;
        }
        if (key instanceof Integer) {
            int keyVal = ((Integer) key).intValue();
            return remove(keyVal);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putAll(Map<? extends Integer, ? extends T> m) {
        if (m instanceof IntMap<?>) {
            this.putAll((IntMap<T>) m);
            return;
        }
        for (Entry<? extends Integer, ? extends T> e : m.entrySet()) {
            int k = e.getKey();
            put(k, e.getValue());
        }
    }

    @Override
    public void clear() {
        last = -1;
        Arrays.fill(keys, Integer.MAX_VALUE);
        Arrays.fill(vals, null);
    }

    @Override
    public IntSet keySet() {
        if (isEmpty()) {
            return IntSet.EMPTY;
        }
        checkSort();
        IntSetArray arr = new IntSetArray(keys, last + 1, true);
        return arr.readOnlyView();
    }

    @Override
    @SuppressWarnings("unchecked")
    public int removeIf(Predicate<T> test) {
        IntSet indices = new IntSetImpl(last + 1);
        for (int i = 0; i < last + 1; i++) {
            T obj = (T) vals[i];
            if (test.test(obj)) {
                indices.add(i);
            }
        }
        removeIndices(indices);
        return indices.size();
    }

    @Override
    public PrimitiveIterator.OfInt keysIterator() {
        return new KeyIter();
    }

    @Override
    public void forEachKey(IntConsumer cons) {
        checkSort();
        for (int i = 0; i <= last; i++) {
            cons.accept(keys[i]);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<T> values() {
        return new ValueCollection();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Entry<Integer, T>> entrySet() {
        int max = size();
        ME[] mes = (ME[]) Array.newInstance(ME.class, max);
        for (int i = 0; i < max; i++) {
            mes[i] = new ME(i);
        }
        return new ArrayBinarySet<>(false, false, new ComparableComparator(), mes);
    }

    @Override
    public int hashCode() {
        // Will produce the same result as AbstractMap / HashMap
        int h = 0;
        for (int i = 0; i <= last; i++) {
            h += keys[i] ^ (vals[i] == null ? 0 : vals[i].hashCode());
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
        if (o instanceof IntMap<?>) {
            IntMap<?> im = (IntMap<?>) o;
            return size() == im.size()
                    && Arrays.equals(keysArray(), im.keysArray())
                    && Arrays.equals(valuesArray(), im.valuesArray());
        }
        if (o instanceof Map<?, ?>) {
            if (((Map<?, ?>) o).size() == size()) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                    if (e.getKey() instanceof Integer) {
                        return Objects.equals(e.getValue(), get(e.getKey()));
                    }
                }
            }
        }
        return false;
    }

    private class KeyIter implements Iterator<Integer>, PrimitiveIterator.OfInt {

        private int ix = -1;

        KeyIter() {
            checkSort();
        }

        @Override
        public boolean hasNext() {
            if (resort) {
                throw new ConcurrentModificationException();
            }
            return ix < size() - 1;
        }

        @Override
        public Integer next() {
            return nextInt();
        }

        @Override
        public int nextInt() {
            return keys[++ix];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private class ArrIter implements Iterator<T> {

        private int ix = -1;

        @Override
        public boolean hasNext() {
            return ix < size() - 1;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            return (T) vals[++ix];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

    private class Iter implements Iterator<Map.Entry<Integer, T>> {

        int ix = -1;

        @Override
        public boolean hasNext() {
            return ix < size() - 1;
        }

        @Override
        public Map.Entry<Integer, T> next() {
            return new ME(++ix);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    private class ME implements Map.Entry<Integer, T>, Comparable<ME> {

        private final int ix;

        ME(int ix) {
            this.ix = ix;
        }

        @Override
        public Integer getKey() {
            return keys[ix];
        }

        @Override
        @SuppressWarnings("unchecked")
        public T getValue() {
            return (T) vals[ix];
        }

        @Override
        @SuppressWarnings("unchecked")
        public T setValue(T v) {
            T old = (T) vals[ix];
            vals[ix] = v;
            return old;
        }

        @Override
        public String toString() {
            return "ME-" + ix + " " + getKey() + " = " + getValue() + " of " + size();
        }

        ArrayIntMap map() {
            return ArrayIntMap.this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o == this) {
                return true;
            }
            if (o.getClass() == getClass()) {
                return ((ME) o).map() == map() && ((ME) o).ix == ix;
            }
            if (o instanceof Map.Entry<?, ?>) {
                Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
                return Objects.equals(getKey(), other.getKey())
                        && Objects.equals(getValue(), other.getValue());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return ix;
        }

        @Override
        public int compareTo(ME o) {
            return Integer.compare(ix, o.ix);
        }
    }

    private void sort() {
        Sort.biSort(keys, vals, size());
    }

    private final class ValsIter implements Iterator<T> {

        int ix = -1;

        @Override
        public boolean hasNext() {
            if (resort) {
                throw new ConcurrentModificationException();
            }
            return ix < size() - 1;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            if (resort) {
                throw new ConcurrentModificationException();
            }
            return (T) vals[++ix];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    class ValueCollection implements Collection<T> {

        @Override
        public int size() {
            return ArrayIntMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return ArrayIntMap.this.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            for (int i = 0; i < size(); i++) {
                if (Objects.equals(vals[i], o)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Iterator<T> iterator() {
            return new ValsIter();
        }

        @Override
        public Object[] toArray() {
            Object[] nue = new Object[size()];
            System.arraycopy(vals, 0, nue, 0, nue.length);
            return nue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            if (a.length != size()) {
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), size());
            }
            System.arraycopy(vals, 0, a, 0, a.length);
            return a;
        }

        @Override
        public boolean add(T e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings("element-type-mismatch")
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null) {
                return false;
            }
            if (o instanceof Collection<?>) {
                Collection<?> c = (Collection<?>) o;
                if (c.size() == size()) {
                    for (Object o1 : c) {
                        if (!contains(o1)) {
                            return false;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hashCode = 1;
            for (int i = 0; i <= last; i++) {
                hashCode = 31 * hashCode + (vals[i] == null ? 0 : vals[i].hashCode());
            }
            return hashCode;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i <= last; i++) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(vals[i]);
            }
            return sb.append(']').toString();
        }
    }
}
