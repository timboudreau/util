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

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
final class SupplierMap<T, R> implements Map<T, R> {

    private final Map<T, R> delegate;
    private final Supplier<R> supplier;

    SupplierMap(Supplier<R> supplier) {
        this.supplier = notNull("supplier", supplier);
        delegate = new HashMap<>();
    }

    SupplierMap(Supplier<R> supplier, Map<T, R> delegate) {
        this.supplier = notNull("supplier", supplier);
        this.delegate = notNull("delegate", delegate);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R get(Object key) {
        R result = delegate.get(key);
        if (result == null) {
            result = supplier.get();
            delegate.put((T) key, result);
        }
        return result;
    }

    /**
     * Performs the traditional Map.get() without potentially
     * inserting an item using the supplier.
     *
     * @param key The key
     * @return A result or null
     */
    @SuppressWarnings("element-type-mismatch")
    public R getIfPresent(Object key) {
        //noinspection SuspiciousMethodCalls
        return delegate.get(key);
    }

    @Override
    public R put(T key, R value) {
        return delegate.put(key, value);
    }

    @Override
    public R remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends T, ? extends R> m) {
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<T> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<R> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<T, R>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public R getOrDefault(Object key, R defaultValue) {
        return delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super T, ? super R> action) {
        delegate.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super T, ? super R, ? extends R> function) {
        delegate.replaceAll(function);
    }

    @Override
    public R putIfAbsent(T key, R value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return delegate.remove(key, value);
    }

    @Override
    public boolean replace(T key, R oldValue, R newValue) {
        return delegate.replace(key, oldValue, newValue);
    }

    @Override
    public R replace(T key, R value) {
        return delegate.replace(key, value);
    }

    @Override
    public R computeIfAbsent(T key, Function<? super T, ? extends R> mappingFunction) {
        return delegate.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public R computeIfPresent(T key, BiFunction<? super T, ? super R, ? extends R> remappingFunction) {
        return delegate.computeIfPresent(key, remappingFunction);
    }

    @Override
    public R compute(T key, BiFunction<? super T, ? super R, ? extends R> remappingFunction) {
        return delegate.compute(key, remappingFunction);
    }

    @Override
    public R merge(T key, R value, BiFunction<? super R, ? super R, ? extends R> remappingFunction) {
        return delegate.merge(key, value, remappingFunction);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(super.toString()).append("[");
        for (Iterator<Map.Entry<T, R>> it = delegate.entrySet().iterator(); it.hasNext();) {
            Map.Entry<T, R> e = it.next();
            result.append(e.getKey()).append("=").append(e.getValue());
            if (it.hasNext()) {
                result.append(", ");
            }
        }
        return result.append(']').toString();
    }
}
