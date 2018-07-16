/*
 * The MIT License
 *
 * Copyright 2018 tim.
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

import com.mastfrog.util.strings.Strings;
import static com.mastfrog.util.collections.CollectionUtils.toList;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
class ArrayBinaryMap<T, R> implements Map<T, R> {

    final Keys keys;
    private final Object[] vals;

    ArrayBinaryMap(Class<T> type, Comparator<T> comparator, int initialCapacity) {
        keys = new Keys(comparator, 10, type);
        vals = new Object[initialCapacity];
    }

    int end() {
        return keys.end;
    }

    class Keys extends ArrayBinarySetMutable<T> {

        public Keys(Comparator<? super T> comp, int initialCapacity, Class<T> type) {
            super(true, comp, initialCapacity, type);
        }

        @Override
        void grow(int newSize) {
            super.grow(newSize);
            Object[] nue = new Object[newSize];
            System.arraycopy(vals, 0, nue, 0, size());
        }

        @Override
        void reSort() {
            System.out.println("resort");
            sort(keys.objs, vals, 0, end(), comp);
        }

        @Override
        void rangeRemoved(int index, int length, int origEnd) {
            System.out.println("Range removed " + length + " at " + index + " orig end " + origEnd);
            int amt = origEnd - (index + length);
            System.out.println("SLIDE OVER " + (index + length) + " to " + index + " moving " + amt);

            System.arraycopy(objs, index + length, objs, index, amt);
//System.arraycopy(objs, rangeStart + rangeLength, objs, dest, num);

            System.out.println("KEYS ARE " + this);
            System.out.println("VALS ARE " + Strings.join(',', vals));
            System.out.println("NOW: " + ArrayBinaryMap.this);
        }
    }

    @Override
    public int size() {
        return keys.size();
    }

    @Override
    public boolean isEmpty() {
        return keys.isEmpty();
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean containsKey(Object key) {
        return keys.contains(key);
    }

    @Override
    public boolean containsValue(Object value) {
        for (int i = 0; i <= end(); i++) {
            if (Objects.equals(value, vals[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public R get(Object key) {
        int ix = keys.indexOf(key);
        if (ix >= 0) {
            return (R) vals[ix];
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public R put(T key, R value) {
        if (!keys.add(key)) {
            int index = keys.indexOf(key);
            R result = (R) vals[index];
//            System.out.println("Replace value for index " + index + " / " + key + " with "
//                    + value + " (was " + result + ")");
            vals[index] = value;
            return result;
        } else {
//            System.out.println("Set value at " + end() + "/" + key + " to " + value + "");
            vals[end()] = value;
        }
        System.out.println("PUT " + key + " " + value + " -> " + this);
        return null;
    }

    @Override
    @SuppressWarnings({"unchecked", "element-type-mismatch"})
    public R remove(Object key) {
        int ix = keys.indexOf(key);
        if (ix >= 0) {
            R result = (R) vals[ix];
//            System.out.println("REMOVE " + result + " at " + ix + " for " + key + " with value " + vals[ix]);
            keys.shiftLeft(ix, ix + 1, (end() - ix));
            return result;
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends T, ? extends R> m) {
        for (Entry<? extends T, ? extends R> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        keys.clear();
        Arrays.fill(vals, null);
    }

    @Override
    public Set<T> keySet() {
        return keys;
    }

    public String toString() {
        StringBuilder result = new StringBuilder(getClass().getSimpleName()).append('@').append(
                Integer.toHexString(System.identityHashCode(this)))
                .append('{');
        for (int i = 0; i <= end(); i++) {
            result.append(keys.objs[i])
                    .append('=').append(vals[i]);
            if (i != end()) {
                result.append(", ");
            }
        }
        return result.append('}').toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<R> values() {
        return (Collection<R>) toList(vals);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Entry<T, R>> entrySet() {
        En[] ens = (En[]) Array.newInstance(En.class, size());
//        keys.checkSort();
        for (int i = 0; i < ens.length; i++) {
            ens[i] = new En(i);
        }
        return new ArrayBinarySet(false, true, new CollectionUtils.ComparableComparator<>(), (Object[]) ens);
    }

    final class En implements Entry<T, R>, Comparable<En> {

        private final int index;

        public En(int index) {
            this.index = index;
        }

        @Override
        public T getKey() {
            return keys.objs[index];
        }

        @Override
        @SuppressWarnings("unchecked")
        public R getValue() {
            return (R) vals[index];
        }

        @Override
        @SuppressWarnings("unchecked")
        public R setValue(R value) {
            R old = (R) vals[index];
            vals[index] = value;
            return old;
        }

        ArrayBinaryMap<T, R> mp() {
            return ArrayBinaryMap.this;
        }

        @Override
        public int compareTo(En o) {
            if (o == this) {
                return 0;
            }
            if (mp() == o.mp()) {
                return index == o.index ? 0 : index > o.index ? 1 : -1;
            }
            T key = keys.objs[index];
            T other = o.getKey();
            return keys.comp.compare(key, other);
        }

        @Override
        public String toString() {
            return index + ": " + getKey() + " = " + getValue();
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null) {
                return false;
            } else if (o instanceof Map.Entry<?, ?>) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                return Objects.equals(getKey(), e.getKey())
                        && Objects.equals(getValue(), e.getValue());
            }
            return false;
        }

        public int hashCode() {
            return getKey().hashCode() + 23 * getValue().hashCode();
        }
    }

    private static <T> void sort(T[] a, Object[] with, int fromIndex, int toIndex, Comparator<T> comp) {
        sort1(a, with, fromIndex, toIndex - fromIndex, comp);
    }

    private static <T> void sort1(T[] x, Object[] with, int off, int len, Comparator<T> comp) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++) {
                for (int j = i; j > off && comp.compare(x[j - 1], x[j]) >= 1; j--) {
                    swap(x, with, j, j - 1, comp);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1);       // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, with, l, l + s, l + 2 * s, comp);
                m = med3(x, with, m - s, m, m + s, comp);
                n = med3(x, with, n - 2 * s, n - s, n, comp);
            }
            m = med3(x, with, l, m, n, comp); // Mid-size, med of 3
        }
        T v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && isLessOrEqual(x[b], v, comp)) {
                if (x[b] == v) {
                    swap(x, with, a++, b, comp);
                }
                b++;
            }
            while (c >= b && isGreaterOrEqual(x[c], v, comp)) {
                if (x[c] == v) {
                    swap(x, with, c, d--, comp);
                }
                c--;
            }
            if (b > c) {
                break;
            }
            swap(x, with, b++, c--, comp);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, with, off, b - s, s, comp);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, with, b, n - s, s, comp);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            sort1(x, with, off, s, comp);
        }
        if ((s = d - c) > 1) {
            sort1(x, with, n - s, s, comp);
        }
    }

    private static <T> void swap(T[] x, Object[] with, int a, int b, Comparator<T> comp) {
        T t = x[a];
        x[a] = x[b];
        x[b] = t;
        Object w = with[a];
        with[a] = with[b];
        with[b] = w;
    }

    private static <T> void vecswap(T x[], Object[] with, int a, int b, int n, Comparator<T> comp) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(x, with, a, b, comp);
        }
    }

    private static <T> int med3(T x[], Object[] with, int a, int b, int c, Comparator<T> comp) {
        return (isLess(x[a], x[b], comp)
                ? (isLess(x[b], x[c], comp) ? b : isLess(x[a], x[c], comp) ? c : a)
                : (isGreater(x[b], x[c], comp) ? b : isGreater(x[a], x[c], comp) ? c : a));
    }

    private static <T> boolean isLessOrEqual(T a, T b, Comparator<T> comp) {
        return comp.compare(a, b) <= 0;
    }

    private static <T> boolean isLess(T a, T b, Comparator<T> comp) {
        return comp.compare(a, b) < 0;
    }

    private static <T> boolean isGreater(T a, T b, Comparator<T> comp) {
        return comp.compare(a, b) > 0;
    }

    private static <T> boolean isGreaterOrEqual(T a, T b, Comparator<T> comp) {
        return comp.compare(a, b) >= 0;
    }

    private static <T> boolean isEq(T a, T b, Comparator<T> comp) {
        return comp.compare(a, b) == 0;
    }
}
