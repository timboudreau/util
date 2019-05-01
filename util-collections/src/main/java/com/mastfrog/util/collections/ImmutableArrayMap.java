/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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

import com.mastfrog.abstractions.list.LongResolvable;
import static com.mastfrog.util.collections.CollectionUtils.setOf;
import com.mastfrog.util.search.Bias;
import com.mastfrog.util.search.BinarySearch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

/**
 * A map backed by two arrays, which uses binary search to locate keys.
 *
 * @since 2.0.0
 * @author Tim Boudreau
 */
class ImmutableArrayMap<T, R> implements Map<T, R>, LongFunction<T> {

    final T[] keys;
    final R[] values;
    final BinarySearch<T> search;
    private final ToLongFunction<? super Object> func;
    private final Class<T> keyType;

    ImmutableArrayMap(Map<T, R> map, Class<T> keyType, Class<R> valType, LongResolvable func) {
        this.keyType = keyType;
        List<Map.Entry<T, R>> l = new ArrayList<>(map.entrySet());
        Collections.sort(l, (Entry<T, ?> o1, Entry<T, ?> o2) -> {
            long a = func.indexOf(o1.getKey());
            long b = func.indexOf(o2.getKey());
            return a == b ? 0 : a > b ? 1 : -1;
        });
        keys = CollectionUtils.genericArray(keyType, l.size());
        values = CollectionUtils.genericArray(valType, l.size());
        long lastValue = Long.MIN_VALUE;
        for (int i = 0; i < keys.length; i++) {
            Map.Entry<T, R> e = l.get(i);
            keys[i] = e.getKey();
            values[i] = e.getValue();
            long val = func.indexOf(keys[i]);
            if (val < lastValue) {
                throw new IllegalArgumentException("Function does not match sort order - "
                        + "applyAsLong() on '" + keys[i - 1] + "' returned " + lastValue + " but "
                        + "applyAsLong() on '" + keys[i] + "' returns " + val);
            }
            lastValue = val;
        }
        ToLongFunction<? super Object> fn = func::indexOf;
        search = new BinarySearch<>(fn, keys.length, this);
        this.func = fn;
    }

    @Override
    public int size() {
        return keys.length;
    }

    @Override
    public boolean isEmpty() {
        return keys.length == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        if (keyType.isInstance(key)) {
            T k = keyType.cast(key);
            long offset = search.search(func.applyAsLong(k), Bias.NONE);
            return offset >= 0;
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Object o : values) {
            if (Objects.equals(o, value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public R get(Object key) {
        if (keyType.isInstance(key)) {
            T k = keyType.cast(key);
            long index = search.search(func.applyAsLong(k), Bias.NONE);
            if (index >= 0) {
                return values[(int) index];
            }
        }
        return null;
    }

    @Override
    public R put(T key, R value) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public R remove(Object key) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void putAll(Map<? extends T, ? extends R> m) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Set<T> keySet() {
        return setOf(keys);
    }

    @Override
    public Collection<R> values() {
        return Arrays.asList(values);
    }

    @Override
    public Set<Entry<T, R>> entrySet() {
        Set<Entry<T, R>> result = new LinkedHashSet<>();
        for (int i = 0; i < keys.length; i++) {
            result.add(new E(i));
        }
        return result;
    }

    @Override
    public T apply(long value) {
        return keys[(int) value];
    }

    final class E implements Map.Entry<T, R> {

        private final int index;

        public E(int index) {
            this.index = index;
        }

        @Override
        public T getKey() {
            return keys[index];
        }

        @Override
        public R getValue() {
            return values[index];
        }

        @Override
        public R setValue(R value) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public String toString() {
            return keys[index] + " = " + values[index];
        }

        @Override
        public int hashCode() {
            return index;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Map.Entry<?, ?>
                    && Objects.equals(((Map.Entry<?, ?>) o).getKey(), getKey())
                    && Objects.equals(((Map.Entry<?, ?>) o).getValue(), getValue());
        }

    }

}
