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

import com.mastfrog.util.sort.Sort;
import com.mastfrog.util.collections.CollectionUtils.ComparableComparator;
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
import java.util.function.IntConsumer;
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

    private int[] keys;

    private Object[] vals;
    private int last = -1;
    private final Supplier<T> emptyValue;
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
    public ArrayIntMap() {
        keys = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE,
            Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        vals = new Object[5];
        emptyValue = null;
    }

    public ArrayIntMap(int minCapacity) {
        this(minCapacity, true, null);
    }

    public ArrayIntMap(int minCapacity, boolean addSuppliedValues, Supplier<T> emptyValue) {
        if (minCapacity <= 0) {
            throw new IllegalArgumentException("Must be > 0");
        }
        this.addSuppliedValues = addSuppliedValues;
        this.emptyValue = emptyValue;
        keys = new int[minCapacity];
        vals = new Object[minCapacity];
        Arrays.fill(keys, Integer.MAX_VALUE);
    }

    public ArrayIntMap(Map<Integer, T> from) {
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
            nextKey = max+1;
            this.keys = Arrays.copyOf(keys, keys.length);
            this.vals = Arrays.copyOf(vals, vals.length);
            this.last = this.vals.length-1;
            emptyValue = null;
        } else {
            last=-1;
            this.keys = new int[5];
            this.vals = new Object[5];
            nextKey = Integer.MIN_VALUE;
            emptyValue = null;
        }
    }

    @Override
    public int[] keysArray() {
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
    @SuppressWarnings("unchecked")
    public void forEach(IntMapConsumer<? super T> cons) {
        for (int i = 0; i < size(); i++) {
            cons.accept(keys[i], (T) this.vals[i]);
        }
    }

    public int[] keys() {
        checkSort();
        int[] result = new int[size()];
        System.arraycopy(keys, 0, result, 0, result.length);
        return result;
    }

    @Override
    public int lowestKey() {
        checkSort();
        return isEmpty() ? -1 : keys[0];
    }

    @Override
    public int highestKey() {
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
            if (last > -1) {
                checkSort();
                if (key < keys[0] || key > keys[last]) {
                    return false;
                }
            }
            if (this.emptyValue == null) {
                return this.get(key) != null;
            } else {
                return this.getIfPresent(key, null) != null;
            }
        }
    }

    @Override
    public int nearest(int key, boolean backward) {
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
    public int[] getKeys() {
        if (last == -1) {
            return new int[0];
        }
        checkSort();
        if (last == -1) {
            return new int[0];
        }
        if (last == keys.length - 1) {
            growArrays();
        }
        int[] result = new int[last + 1];
        try {
            System.arraycopy(keys, 0, result, 0, last + 1);
            return result;
        } catch (ArrayIndexOutOfBoundsException aioobe) { //XXX temp diagnostics
            ArrayIndexOutOfBoundsException e = new ArrayIndexOutOfBoundsException(
                    "AIOOBE in IntMap.getKeys() - last = " + last + " keys: "
                    + i2s(keys) + " vals: " + Arrays.asList(vals) + " result length "
                    + result.length);
            e.initCause(aioobe);
            throw e;
        }
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

    int nextKey = 0;
    boolean resort;

    @SuppressWarnings("unchecked")
    @Override
    public T put(int key, T val) {
        boolean between = false;
        if (last >= 0) {
            if (keys[last] == key && vals[last] == val) {
                return val;
            } else if (keys[last] == key) {
                T old = (T) vals[last];
                vals[last] = val;
                return old;
            }
            if (key < keys[last]) {
                resort = true;
                between = key >= keys[0];
            } else if (key == keys[last]) {
                between = true;
            }
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
    }

    void checkSort() {
        if (resort) {
            sort();
            resort = false;
        }
    }

    private void growArrays() {
        int newSize = last < 250 ? last + Math.max(5, last + (last / 3)) : last + (last / 2);
        int[] newKeys = new int[newSize];
        Object[] newVals = new Object[newSize];
        System.arraycopy(keys, 0, newKeys, 0, keys.length);
        System.arraycopy(vals, 0, newVals, 0, vals.length);
        Arrays.fill(newKeys, keys.length, newKeys.length, Integer.MAX_VALUE);
        keys = newKeys;
        vals = newVals;
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
        } else if (entry > keys[last]) {
            return -1;
        }
        checkSort();
        int result = -1;
        if (!isEmpty()) {
            int idx = Arrays.binarySearch(keys, 0, last + 1, entry);
            if (idx >= 0) {
                result = idx == keys.length - 1 ? keys[0] : keys[idx + 1];
            }
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

        Arrays.fill(keys, last - shift + 1, last + 1, Integer.MAX_VALUE);
        last = last - shift;
    }

    @Override
    public Iterable<Map.Entry<Integer, T>> entries() {
        return this;
    }

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
        if (key == null) {
            throw new IllegalArgumentException("Null not allowed as a key");
        }
        return this.put(key.intValue(), value);
    }

    @SuppressWarnings("unchecked")
    public T remove(int keyVal) {
        if (last >= 0 && keyVal == keys[last]) {
            T old = (T) vals[last];
            vals[last] = null;
            last--;
            return old;
        }
        int index = Arrays.binarySearch(keys, 0, last + 1, keyVal);
        if (index >= 0) {
            T result = (T) vals[index];
            if (index == 0) {
                int[] newKeys = new int[keys.length];
                Object[] newVals = new Object[vals.length];
                System.arraycopy(keys, 1, newKeys, 0, keys.length - 1);
                System.arraycopy(vals, 1, newVals, 0, vals.length - 1);
                newKeys[newKeys.length - 1] = Integer.MAX_VALUE;
                last--;
                keys = newKeys;
                vals = newVals;
            } else if (index == last) {
                keys[last] = Integer.MAX_VALUE;
                vals[last] = null;
                last--;
                return result;
            } else {
                int[] newKeys = new int[keys.length];
                Object[] newVals = new Object[vals.length];
                System.arraycopy(keys, 0, newKeys, 0, index);
                System.arraycopy(vals, 0, newVals, 0, index);
                System.arraycopy(keys, index + 1, newKeys, index, keys.length - (index + 1));
                System.arraycopy(vals, index + 1, newVals, index, vals.length - (index + 1));
                newKeys[newKeys.length - 1] = Integer.MAX_VALUE;
                newVals[newVals.length - 1] = null;
                last--;
                keys = newKeys;
                vals = newVals;
            }
            return result;
        }
        return null;
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
    public void putAll(Map<? extends Integer, ? extends T> m) {
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
    public Set<Integer> keySet() {
        return new KeySet();
    }

    @Override
    public PrimitiveIterator.OfInt keysIterator() {
        return new KeyIter();
    }

    @Override
    public void forEachKey(IntConsumer cons) {
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

        public ME(int ix) {
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

    private class KeySet implements Set<Integer> {

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(size() * 3).append('[');
            for (int i = 0; i < last + 1; i++) {
                sb.append(keys[i]);
                if (i != last) {
                    sb.append(", ");
                }
            }
            return sb.append(']').toString();
        }

        @Override
        public int size() {
            return last + 1;
        }

        @Override
        public boolean isEmpty() {
            return last == -1;
        }

        @Override
        @SuppressWarnings("element-type-mismatch")
        public boolean contains(Object o) {
            return containsKey(o);
        }

        @Override
        public Iterator<Integer> iterator() {
            return new KeyIter();
        }

        ArrayIntMap map() {
            return ArrayIntMap.this;
        }

        @SuppressWarnings({"unchecked", "element-type-mismatch"})
        @Override
        public boolean equals(Object o) {
            if (o != null && o.getClass() == getClass()) {
                return ((KeySet) o).map() == map();
            }
            if (o instanceof Collection<?>) {
                if (size() == ((Collection<?>) o).size()) {
                    for (Object o1 : ((Collection<?>) o)) {
                        if (!contains(o1)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int h = 0;
            for (int i = 0; i <= last; i++) {
                h += keys[i];
            }
            return h;
        }

        @Override
        public Object[] toArray() {
            Object[] result = new Object[last + 1];
            for (int i = 0; i <= last; i++) {
                result[i] = keys[i];
            }
            return result;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            if (a.length != last + 1) {
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), last + 1);
            }
            for (int i = 0; i < a.length; i++) {
                a[i] = (T) Integer.valueOf(keys[i]);
            }
            return a;
        }

        @Override
        public boolean add(Integer e) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Object o : c) {
                if (!containsKey(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends Integer> c) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    class ValsIter implements Iterator<T> {

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

        public boolean equals(Object o) {
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
