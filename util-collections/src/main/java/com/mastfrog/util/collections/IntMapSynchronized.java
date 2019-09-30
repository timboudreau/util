/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
import java.util.Iterator;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 *
 * @author Tim Boudreau
 */
final class IntMapSynchronized<T> implements IntMap<T> {

    private final IntMap<T> delegate;

    public IntMapSynchronized(IntMap<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized boolean containsKey(int key) {
        return delegate.containsKey(key);
    }

    @Override
    public synchronized void decrementKeys(int decrement) {
        delegate.decrementKeys(decrement);
    }

    @Override
    public synchronized Iterable<Entry<Integer, T>> entries() {
        return delegate.entries();
    }

    @Override
    public synchronized T get(int key) {
        return delegate.get(key);
    }

    @Override
    public synchronized int[] getKeys() {
        return delegate.getKeys();
    }

    @Override
    public synchronized int highestKey() {
        return delegate.highestKey();
    }

    @Override
    public PrimitiveIterator.OfInt keysIterator() {
        return new SyncPrimIterator(this);
    }

    @Override
    public synchronized T getIfPresent(int key, T defaultValue) {
        return delegate.getIfPresent(key, defaultValue);
    }

    static final class SyncPrimIterator implements PrimitiveIterator.OfInt {

        private final PrimitiveIterator.OfInt delegate;
        private final Object lock;

        public SyncPrimIterator(IntMapSynchronized<?> map) {
            this.lock = map;
            delegate = map.delegate.keysIterator();
        }

        @Override
        public int nextInt() {
            synchronized (lock) {
                return delegate.nextInt();
            }
        }

        @Override
        public boolean hasNext() {
            synchronized (lock) {
                return delegate.hasNext();
            }
        }
    }

    @Override
    public synchronized int lowestKey() {
        return delegate.lowestKey();
    }

    @Override
    public synchronized int nearest(int key, boolean backward) {
        return delegate.nearest(key, backward);
    }

    @Override
    public synchronized T put(int key, T val) {
        return delegate.put(key, val);
    }

    @Override
    public synchronized IntMap<T> toSynchronizedIntMap() {
        return this;
    }

    @Override
    public synchronized void forEachKey(IntConsumer cons) {
        delegate.forEachKey(cons);
    }

    @Override
    public synchronized void forEach(IntMapConsumer<? super T> cons) {
        delegate.forEach(cons);
    }

    @Override
    public synchronized boolean forSomeKeys(IntMapAbortableConsumer<? super T> cons) {
        return delegate.forSomeKeys(cons);
    }

    @Override
    public synchronized Iterator<Entry<Integer, T>> iterator() {
        return delegate.iterator();
    }

    @Override
    public synchronized void forEach(Consumer<? super Entry<Integer, T>> action) {
        delegate.forEach(action);
    }

    @Override
    public synchronized Spliterator<Entry<Integer, T>> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public synchronized int size() {
        return delegate.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public synchronized T get(Object key) {
        return delegate.get(key);
    }

    @Override
    public synchronized T put(Integer key, T value) {
        return delegate.put(key, value);
    }

    @Override
    public synchronized T remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public synchronized void putAll(Map<? extends Integer, ? extends T> m) {
        delegate.putAll(m);
    }

    @Override
    public synchronized void clear() {
        delegate.clear();
    }

    @Override
    public synchronized Set<Integer> keySet() {
        return delegate.keySet();
    }

    @Override
    public synchronized Collection<T> values() {
        return delegate.values();
    }

    @Override
    public synchronized Set<Entry<Integer, T>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public synchronized T getOrDefault(Object key, T defaultValue) {
        return delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public synchronized void forEach(BiConsumer<? super Integer, ? super T> action) {
        delegate.forEach(action);
    }

    @Override
    public synchronized void replaceAll(BiFunction<? super Integer, ? super T, ? extends T> function) {
        delegate.replaceAll(function);
    }

    @Override
    public synchronized T putIfAbsent(Integer key, T value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        return delegate.remove(key, value);
    }

    @Override
    public synchronized boolean replace(Integer key, T oldValue, T newValue) {
        return delegate.replace(key, oldValue, newValue);
    }

    @Override
    public synchronized T replace(Integer key, T value) {
        return delegate.replace(key, value);
    }

    @Override
    public synchronized T computeIfAbsent(Integer key, Function<? super Integer, ? extends T> mappingFunction) {
        return delegate.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public synchronized T computeIfPresent(Integer key, BiFunction<? super Integer, ? super T, ? extends T> remappingFunction) {
        return delegate.computeIfPresent(key, remappingFunction);
    }

    @Override
    public synchronized T compute(Integer key, BiFunction<? super Integer, ? super T, ? extends T> remappingFunction) {
        return delegate.compute(key, remappingFunction);
    }

    @Override
    public synchronized T merge(Integer key, T value, BiFunction<? super T, ? super T, ? extends T> remappingFunction) {
        return delegate.merge(key, value, remappingFunction);
    }

    @Override
    public synchronized int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public synchronized boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public synchronized String toString() {
        return delegate.toString();
    }
}
