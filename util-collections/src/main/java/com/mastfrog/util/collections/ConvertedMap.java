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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
class ConvertedMap<From, T, R, F2 extends From> implements Map<From, R> {

    private final Class<F2> from;

    private final Map<T, R> delegate;
    private final Converter<T, F2> converter;

    public ConvertedMap(Class<F2> from, Map<T, R> delegate, Converter<T, F2> converter) {
        this.from = from;
        this.delegate = delegate;
        this.converter = converter;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    protected boolean isKey(Object key) {
        return from.isInstance(key);
    }

    @Override
    public boolean containsKey(Object key) {
        if (isKey(key)) {
            F2 f = from.cast(key);
            T realKey = converter.convert(f);
            return delegate.containsKey(realKey);
        }
        return false;
    }

    private T toKey(Object key) {
        if (key == null) {
            return null;
        }
        if (isKey(key)) {
            F2 f = from.cast(key);
            return converter.convert(f);
        }
        return null;
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public R get(Object key) {
        if (key == null) {
            return delegate.get(null);
        }
        T realKey = toKey(key);
        return realKey == null ? null : delegate.get(realKey);
    }

    @Override
    public R put(From key, R value) {
        T realKey = key == null ? null : toKey(key);
        return delegate.put(realKey, value);
    }

    @Override
    public R remove(Object key) {
        T realKey = toKey(key);
        return delegate.remove(realKey);
    }

    @Override
    public void putAll(Map<? extends From, ? extends R> m) {
        for (Entry<? extends From, ? extends R> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<From> keySet() {
        Set<From> result = new HashSet<>(size());
        for (T key : delegate.keySet()) {
            result.add(converter.unconvert(key));
        }
        return result;
    }

    @Override
    public Collection<R> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<From, R>> entrySet() {
        Set<Entry<From, R>> entries = new HashSet<>();
        for (Map.Entry<T, R> e : delegate.entrySet()) {
            entries.add(new En(e));
        }
        return entries;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        }
        if (o instanceof Map<?, ?>) {
            Map<?, ?> other = (Map<?, ?>) this;
            if (other.size() != size()) {
                return false;
            }
            for (Map.Entry<?, ?> e : other.entrySet()) {
                if (!Objects.equals(e.getValue(), get(e.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = 0;
        Iterator<Entry<From, R>> i = entrySet().iterator();
        while (i.hasNext()) {
            h += i.next().hashCode();
        }
        return h;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName()).append(System.identityHashCode(this)).append("from=")
                .append(from.getName()).append(", converter=").append(converter).append(",entries={");
        for (Map.Entry<From, R> e : entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append(' ');
        }
        return sb.append('}').toString();
    }

    final class En implements Map.Entry<From, R> {

        private final Map.Entry<T, R> real;

        public En(Entry<T, R> real) {
            this.real = real;
        }

        @Override
        public From getKey() {
            return converter.unconvert(real.getKey());
        }

        @Override
        public R getValue() {
            return real.getValue();
        }

        @Override
        public R setValue(R value) {
            return real.setValue(value);
        }

        @Override
        public int hashCode() {
            From key = getKey();
            R value = getValue();
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final En other = (En) obj;
            return Objects.equals(this.real, other.real);
        }
    }
}
