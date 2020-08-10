/*
 * Copyright 2016-2019 Tim Boudreau, Frédéric Yvon Vinet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastfrog.util.cache;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementation of Cache over a map.
 *
 * @author Tim Boudreau
 */
public final class MapCache<K, V> implements Cache<K, V, RuntimeException> {

    private final Map<K, V> map;
    private final Function<K, V> func;

    public MapCache(Map<K, V> map, Function<K, V> func) {
        this.map = map;
        this.func = func;
    }

    public static <K,V> MapCache<K,V> imperative(Map<K,V> map) {
        return new MapCache(map, ignored -> null);
    }

    public MapCache<K, V> remove(K key) {
        synchronized (this) {
            map.remove(key);
        }
        return this;
    }

    public MapCache<K, V> put(K key, V value) {
        map.put(key, value);
        return this;
    }

    @Override
    public V get(K key) {
        V result = map.get(key);
        if (result == null) {
            V nue = func.apply(key);
            synchronized (this) {
                result = map.get(key);
                if (result == null) {
                    if (nue != null) {
                        map.put(key, nue);
                    }
                    result = nue;
                }
            }
        }
        return result;
    }

    @Override
    public synchronized Cache<K, V, RuntimeException> clear() {
        map.clear();
        return this;
    }

    @Override
    public void close() {
        clear();
    }

    @Override
    public Optional<V> getOptional(K key) {
        return Optional.ofNullable(map.get(key));
    }

    @Override
    public Optional<V> cachedValue(K key) {
        return getOptional(key);
    }
}
