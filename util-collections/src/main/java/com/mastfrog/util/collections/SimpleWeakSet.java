/*
 * The MIT License
 *
 * Copyright 2019 Tim Boudreau.
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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
class SimpleWeakSet<T> extends AbstractSet<T> {

    private final Map<T, Boolean> backingStore = new WeakHashMap<>();

    SimpleWeakSet() {

    }

    SimpleWeakSet(Collection<? extends T> other) {
        addAll(other);
    }

    @Override
    public String toString() {
        return backingStore.keySet().toString();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return backingStore.keySet().containsAll(c);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return backingStore.keySet().toArray(a);
    }

    @Override
    public Object[] toArray() {
        return backingStore.keySet().toArray();
    }

    @Override
    public boolean contains(Object o) {
        return backingStore.containsKey(o);
    }

    @Override
    public boolean isEmpty() {
        return backingStore.isEmpty();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        backingStore.keySet().forEach(action);
    }

    @Override
    public Iterator<T> iterator() {
        return backingStore.keySet().iterator();
    }

    @Override
    public void clear() {
        backingStore.clear();
    }

    @Override
    public int size() {
        return backingStore.size();
    }

    @Override
    public boolean add(T e) {
        if (e == null) {
            throw new IllegalArgumentException("Null argument to add");
        }
        Boolean result = backingStore.put(e, Boolean.TRUE);
        return result == null ? false : result;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        int oldSize = size();
        for (T t : c) {
            backingStore.put(t, Boolean.TRUE);
        }
        return size() != oldSize;
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            changed |= backingStore.remove(o);
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Set<T> toRemove = new HashSet<>();
        for (Map.Entry<T, ?> e : backingStore.entrySet()) {
            if (!c.contains(e.getKey())) {
                toRemove.add(e.getKey());
            }
        }
        return removeAll(toRemove);
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean remove(Object o) {
        return backingStore.remove(o);
    }
}
