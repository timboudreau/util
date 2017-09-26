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

import com.mastfrog.util.Checks;
import com.mastfrog.util.Strings;
import com.mastfrog.util.collections.HeteroMap.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Map of types to objects with that type. Can be used with either class objects
 * as keys, or instances of Key (which allow the map to contain more than one
 * instance of that type).
 *
 * @author Tim Boudreau
 */
public final class HeteroMap implements Iterable<Map.Entry<Key<?>, Object>> {

    private final Map<Key<?>, Object> map;

    /**
     * Create a new HeteroMap backed by the passed <i>empty</i> map - use this
     * constructor if you need a concurrent or synchronized map.
     *
     * @param internal
     * @throws IllegalArgumentException if the passed map already has contents
     */
    public HeteroMap(Map<Key<?>, Object> internal) {
        Checks.notNull("internal", internal);
        if (!internal.isEmpty()) {
            throw new IllegalArgumentException("This constructor is only for "
                    + "use to provide a synchronized or concurrent map.  The "
                    + "passed map may not already have contents.");
        }
        this.map = internal;
    }

    HeteroMap(boolean internal, Map<Key<?>, Object> map) {
        this.map = map;
    }

    public HeteroMap() {
        this(new HashMap<>());
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public String toString() {
        return '{' + Strings.join(',', map.entrySet()) + '}';
    }

    public Map<String, Object> toStringObjectMap() {
        List<Key> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys, (a, b) -> {
            return a.name.compareTo(b.name);
        });
        Map<String, Object> result = new LinkedHashMap<>();
        for (Key key : keys) {
            result.put(key.name, get(key));
        }
        return result;
    }

    /**
     * Create an independent, duplicate map to this one.
     *
     * @return A copy of this map
     */
    public HeteroMap copy() {
        HeteroMap result = new HeteroMap();
        result.map.putAll(map);
        return result;
    }

    public HeteroMap unmodifiableCopy() {
        Map<Key<?>, Object> newMap = new HashMap<>();
        newMap.putAll(map);
        return new HeteroMap(true, Collections.unmodifiableMap(newMap));
    }

    @SuppressWarnings("unchecked")
    private <T> Key<T> findKey(Class<T> type, boolean create) {
        for (Map.Entry<Key<?>, Object> e : map.entrySet()) {
            Key<?> k = e.getKey();
            if (type == k.type && k.name.equals(type.getName())) {
                return (Key<T>) k;
            }
        }
        if (create) {
            return new Key<>(type, type.getName());
        }
        return null;
    }

    public <T> T remove(Class<T> type) {
        Key<T> key = findKey(type, false);
        if (key != null) {
            return remove(key);
        }
        return null;
    }

    public <T> T remove(Key<T> key) {
        Object o = map.remove(key);
        return o == null ? null : key.type.cast(o);
    }

    /**
     * Get all contents of a particular type.
     *
     * @param <T> The type
     * @param type The type
     * @return A set of objects
     */
    public <T> Set<T> getAllByType(Class<T> type) {
        Set<T> result = new HashSet<>();
        for (Object o : map.values()) {
            if (type.isInstance(o)) {
                result.add(type.cast(o));
            }
        }
        return result;
    }

    /**
     * Look up an object in the map by type.
     *
     * @param <T> The type
     * @param type The type
     * @param defaultValue The default value
     * @return An object or null.
     */
    public <T> T get(Class<T> type, T defaultValue) {
        Checks.notNull("type", type);
        T result = get(type);
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * Get an object of type T from the map.
     *
     * @param <T> The type
     * @param type The type
     * @return An object of type T if any is present, or null.
     */
    public <T> T get(Class<T> type) {
        Checks.notNull("type", type);
        Key<T> key = findKey(type, false);
        return key == null ? null : get(key);
    }

    /**
     * Put a value, creating a key if necessary from the type of the value
     * passed. Note that the resulting key will be tied to the <i>exact type</i>
     * of the object passed.
     *
     * @param <T>
     * @param value
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> Key<T> put(T value) {
        Checks.notNull("value", value);
        Class<T> type = (Class<T>) value.getClass();
        return put(type, value);
    }

    /**
     * Put an object by type.
     *
     * @param <T> The type
     * @param type The type
     * @param value The value, may not be null
     * @return The key used, which may be newly created if no matching key
     * exists in the map
     */
    public <T> Key<T> put(Class<T> type, T value) {
        Checks.notNull("type", type);
        Checks.notNull("value", value);
        Key<T> key = findKey(type, true);
        put(key, value);
        return key;
    }

    /**
     * Get a value, using the passed default if not present.
     *
     * @param <T> The type
     * @param key The key
     * @param defaultValue The fallback value
     * @return The value or the fallback value
     */
    public <T> T get(Key<T> key, T defaultValue) {
        Checks.notNull("key", key);
        T result = get(key);
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * Get an object from the map, cast to the type of the key.
     *
     * @param <T> The type
     * @param key The key
     * @return An object or null
     */
    public <T> T get(Key<T> key) {
        Checks.notNull("key", key);
        return key.type.cast(map.get(key));
    }

    /**
     * Put an item in the map, using the key.
     *
     * @param <T> The type
     * @param key The key
     * @param value The value
     * @return this
     */
    public <T> HeteroMap put(Key<T> key, T value) {
        Checks.notNull("key", key);
        Checks.notNull("value", value);
        map.put(key, key.type.cast(value)); //fail fast
        return this;
    }

    /**
     * Create a new key
     *
     * @param <T> The type
     * @param type The type
     * @param name The name of the key, which is also used for matching - the
     * map may contain two keys with the same type if the names differ, or two
     * keys with the same name if the types differ.
     * @return A new key
     */
    public static final <T> Key<T> newKey(Class<T> type, String name) {
        Checks.notNull("type", type);
        Checks.notNull("name", name);
        return new Key<>(type, name);
    }

    /**
     * Get an iterator over the map entries.
     *
     * @return An iterator
     */
    @Override
    public Iterator<Map.Entry<Key<?>, Object>> iterator() {
        return Collections.unmodifiableSet(map.entrySet()).iterator();
    }

    public static final class Key<T> {

        private final Class<T> type;
        private final String name;

        Key(Class<T> type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public String toString() {
            return name + "(" + type.getName() + ")";
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + Objects.hashCode(this.type);
            hash = 29 * hash + Objects.hashCode(this.name);
            return hash;
        }

        @Override
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
            final Key<?> other = (Key<?>) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            return Objects.equals(this.type, other.type);
        }

    }
}
