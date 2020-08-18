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

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
final class SingletonIntMap<T> implements IntMap<T>, Map.Entry<Integer, T> {

    private final int key;
    private T value;

    SingletonIntMap(int key, T value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void trim() {
        // do nothing
    }

    @Override
    public T leastValue() {
        return value;
    }

    @Override
    public T valueAt(int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("Index out of range for singleton map: "
                    + index);
        }
        return value;
    }

    @Override
    public int indexOf(int key) {
        return key == this.key ? 0 : -1;
    }

    @Override
    public void forEachIndexed(IndexedIntMapConsumer<? super T> c) {
        c.accept(0, key, value);
    }

    @Override
    public void forEachReversed(IndexedIntMapConsumer<? super T> c) {
        c.accept(0, key, value);
    }

    @Override
    public int key(int index) {
        if (index == 0) {
            return key;
        }
        throw new IndexOutOfBoundsException("Singleton map: " + index);
    }

    @Override
    public int removeIndices(IntSet toRemove) {
        if (toRemove.isEmpty()) {
            return 0;
        }
        throw new UnsupportedOperationException("Singleton map");
    }

    @Override
    public int nearestIndexTo(int key, boolean backward) {
        if (backward && key >= this.key) {
            return 0;
        } else if (!backward && key <= this.key) {
            return 0;
        }
        return -1;
    }

    @Override
    public int valuesBetween(int first, int second, IntMapConsumer<T> c) {
        int a = Math.min(first, second);
        int b = Math.max(first, second);
        if (key <= b || key >= a) {
            c.accept(key, value);
            return 1;
        }
        return 0;
    }

    @Override
    public int keysAndValuesBetween(int first, int second, IndexedIntMapConsumer<T> c) {
        int a = Math.min(first, second);
        int b = Math.max(first, second);
        if (key <= b || key >= a) {
            c.accept(0, key, value);
            return 1;
        }
        return 0;
    }

    @Override
    public void setValueAt(int index, T obj) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("Index out of range for singleton map: " + index);
        }
        value = obj;
    }

    @Override
    public T greatestValue() {
        return value;
    }

    @Override
    public boolean containsKey(int key) {
        return this.key == key;
    }

    @Override
    public int removeIf(Predicate<T> test) {
        throw new UnsupportedOperationException("Read only.");
    }

    @Override
    public void decrementKeys(int decrement) {
        throw new UnsupportedOperationException("Read only.");
    }

    @Override
    public Iterable<Entry<Integer, T>> entries() {
        throw new UnsupportedOperationException("Read-only");
    }

    @Override
    public T get(int key) {
        return key == this.key ? value : null;
    }

    @Override
    public T getIfPresent(int key, T defaultValue) {
        return key == this.key ? value : defaultValue;
    }

    @Override
    public int[] keysArray() {
        return new int[]{key};
    }

    @Override
    public int greatestKey() {
        return key;
    }

    @Override
    public PrimitiveIterator.OfInt keysIterator() {
        return new PrimitiveIterator.OfInt() {
            private boolean used;

            @Override
            public int nextInt() {
                if (!used) {
                    used = true;
                    return key;
                }
                throw new NoSuchElementException();
            }

            @Override
            public boolean hasNext() {
                return !used;
            }
        };
    }

    @Override
    public int leastKey() {
        return key;
    }

    @Override
    public int nearestKey(int key, boolean backward) {
        return key;
    }

    @Override
    public T put(int key, T val) {
        if (key == 0) {
            T old = this.value;
            this.value = val;
            return old;
        }
        throw new UnsupportedOperationException("Singleton instance - "
                + "index 0 only one available");
    }

    @Override
    public Iterator<Entry<Integer, T>> iterator() {
        return CollectionUtils.singletonIterator(this);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return Integer.valueOf(this.key).equals(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return Objects.equals(this.value, value);
    }

    @Override
    public T get(Object key) {
        return containsKey(key) ? value : null;
    }

    @Override
    public T put(Integer key, T value) {
        notNull("key", key);
        if (key.intValue() != 0) {
            throw new IndexOutOfBoundsException("Singleton map does not contain "
                    + "index " + key);
        }
        T old = this.value;
        this.value = value;
        return old;
    }

    @Override
    public T remove(Object key) {
        throw new UnsupportedOperationException("Singleton map");
    }

    @Override
    public T removeIndex(int index) {
        throw new UnsupportedOperationException("Singleton map");
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends T> m) {
        if (m.isEmpty()) {
            return;
        }
        if (m.size() == 1) {
            Entry<? extends Integer, ? extends T> e = m.entrySet().iterator().next();
            if (e.getKey() != null && 0 == e.getKey().intValue()) {
                this.value = e.getValue();
                return;
            }
        }
        throw new UnsupportedOperationException("Singleton map");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Read-only");
    }

    @Override
    public IntSet keySet() {
        IntSetImpl result = new IntSetImpl(1);
        result.add(key);
        return result;
    }

    @Override
    public Collection<T> values() {
        return Collections.singleton(value);
    }

    @Override
    public int hashCode() {
        return key ^ (value == null ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
        return "{" + key + "=" + value + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof IntMap<?>) {
            IntMap<?> im = (IntMap<?>) o;
            return im.size() == 1
                    && Objects.equals(value, im.get(key))
                    && im.containsKey(key); // test this or any null value will match
        } else if (o instanceof Map<?, ?>) {
            Map<?, ?> m = (Map<?, ?>) o;
            return m.size() == 1 && m.containsKey(key)
                    && Objects.equals(m.get(key), value);
        }
        return false;
    }

    @Override
    public Set<Entry<Integer, T>> entrySet() {
        return Collections.singleton(this);
    }

    @Override
    public Integer getKey() {
        return key;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public T setValue(T value) {
        T old = this.value;
        this.value = value;
        return old;
    }
}
