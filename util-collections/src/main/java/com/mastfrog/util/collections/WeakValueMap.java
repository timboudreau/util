/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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

import com.mastfrog.util.collections.Trimmable.TrimmableMap;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A map with weak values; internally proxies a
 * <code>Map&lt;K,Reference&lt;V&gt;&gt;</code>.
 *
 * @author Tim Boudreau
 */
final class WeakValueMap<K, V> extends AbstractMap<K, V> implements TrimmableMap<K, V> {

    private final Map<K, Reference<V>> internal;
    private final Function<V, Reference<V>> referenceFactory;

    WeakValueMap(int initialSize) {
        internal = new HashMap<>();
        referenceFactory = WeakReference::new;
    }

    WeakValueMap(MapFactory factory, int initialSize, Function<V, Reference<V>> referenceFactory) {
        internal = factory.createMap(initialSize, false);
        this.referenceFactory = referenceFactory;
    }

    @Override
    public void trim() {
        synchronized (internal) {
            Set<K> toRemove = new HashSet<>();
            for (Entry<K, Reference<V>> e : internal.entrySet()) {
                if (e.getValue().get() == null) {
                    toRemove.add(e.getKey());
                }
            }
            if (!toRemove.isEmpty()) {
                for (K key : toRemove) {
                    internal.remove(key);
                }
            }
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (internal) {
            if (internal.isEmpty()) {
                return true;
            }
            boolean result = size() == 0;
            if (result && !internal.isEmpty()) {
                internal.clear();
            }
            return result;
        }
    }

    @Override
    public int size() {
        int result = 0;
        synchronized (internal) {
            for (Entry<K, Reference<V>> e : internal.entrySet()) {
                if (e.getValue().get() != null) {
                    result++;
                }
            }
        }
        return result;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> result = new HashSet<>();
        synchronized (internal) {
            for (Entry<K, Reference<V>> e : internal.entrySet()) {
                V value = e.getValue().get();
                if (value != null) {
                    result.add(new E(e.getKey(), value));
                }
            }
        }
        return result;
    }

    @Override
    public Set<K> keySet() {
        Set<K> result = new HashSet<>();
        synchronized (internal) {
            for (Entry<K, Reference<V>> e : internal.entrySet()) {
                if (e.getValue().get() != null) {
                    result.add(e.getKey());
                }
            }
        }
        return result;
    }

    @Override
    public void clear() {
        internal.clear();
    }

    @Override
    public V remove(Object key) {
        Reference<V> ref;
        synchronized (internal) {
            ref = internal.remove(key);
        }
        return ref == null ? null : ref.get();
    }

    @Override
    public V replace(K key, V value) {
        Reference<V> old = null;
        synchronized (internal) {
            old = internal.get(key);
            if (old != null) {
                internal.put(key, referenceFactory.apply(value));
            }
        }
        return old == null ? null : old.get();
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        synchronized (internal) {
            Reference<V> old = internal.get(key);
            if (old != null) {
                V val = old.get();
                if (Objects.equals(oldValue, val)) {
                    internal.put(key, referenceFactory.apply(newValue));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean remove(Object key, Object value) {
        synchronized (internal) {
            Reference<V> old = internal.get(key);
            if (old != null) {
                V oldValue = old.get();
                if (Objects.equals(value, oldValue)) {
                    return internal.remove(key) != null;
                }
            }
        }
        return false;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        synchronized (internal) {
            V currentValue = get(key); // creates an ephemeral strong reference
            if (currentValue == null) {
                internal.put(key, referenceFactory.apply(value));
                return null;
            } else {
                return currentValue;
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Set<Entry<K, V>> entries = entrySet(); // block garbage collection of references while iterating
        for (Entry<K, V> e : entries) {
            V nue = function.apply(e.getKey(), e.getValue());
            if (!Objects.equals(e.getValue(), nue)) {
                synchronized (internal) {
                    internal.put(e.getKey(), referenceFactory.apply(nue));
                }
            }
        }
    }

    @Override
    public Collection<V> values() {
        List<V> vals;
        synchronized (internal) {
            if (internal.isEmpty()) {
                vals = Collections.emptyList();
            } else {
                vals = new ArrayList<>(internal.size());
                for (Entry<K, Reference<V>> e : internal.entrySet()) {
                    V val = e.getValue().get();
                    if (val != null) {
                        vals.add(val);
                    }
                }
            }
        }
        return vals;
    }

    @Override
    public boolean containsKey(Object key) {
        Reference<V> ref = null;
        synchronized (internal) {
            // contains test can be faster than lookup
            if (internal.containsKey(key)) {
                ref = internal.get(key);
            }
        }
        return ref != null && ref.get() != null;
    }

    @Override
    public boolean containsValue(Object value) {
        synchronized (internal) {
            if (internal.isEmpty()) {
                return false;
            }
            for (Entry<K, Reference<V>> e : internal.entrySet()) {
                if (Objects.equals(value, e.getValue().get())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public V put(K key, V value) {
        Reference<V> ref = referenceFactory.apply(value);
        synchronized (internal) {
            ref = internal.put(key, ref);
        }
        return ref == null ? null : ref.get();
    }

    @Override
    public V get(Object key) {
        Reference<V> ref;
        synchronized (internal) {
            ref = internal.get(key);
        }
        return ref == null ? null : ref.get();
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public V getOrDefault(Object key, V defaultValue) {
        Reference<V> ref;
        synchronized (internal) {
            ref = internal.get(key);
        }
        if (ref == null) {
            return defaultValue;
        }
        V result = ref.get();
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Iterator<Entry<K, Reference<V>>> internalEntries;
        synchronized (internal) {
            internalEntries = internal.entrySet().iterator();
        }
        while (internalEntries.hasNext()) {
            Entry<K, Reference<V>> entry = internalEntries.next();
            Reference<V> val = entry.getValue();
            V value = val.get();
            if (value != null) {
                action.accept(entry.getKey(), value);
            }
        }
    }

    final class E implements Map.Entry<K, V> {

        private final K key;
        private final V value;

        E(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            synchronized (internal) {
                internal.put(key, referenceFactory.apply(value));
            }
            return this.value;
        }

        @Override
        public final String toString() {
            return key + "=" + value;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        @Override
        public final boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null) {
                return false;
            } else if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                return Objects.equals(key, e.getKey())
                        && Objects.equals(value, e.getValue());
            }
            return false;
        }
    }
}
