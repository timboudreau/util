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
package com.mastfrog.util.collections;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Factory for maps.
 *
 * @author Tim Boudreau
 */
public interface MapFactory {

    /**
     * Create a map.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param size The initial size
     * @param threadSafe If true, return a map wrapped in SynchronizedMap for
     * cases that are not thread-safe by default
     * @return A map
     */
    <T, R> Map<T, R> createMap(int size, boolean threadSafe);

    /**
     * Some cache-like implementations may not actually be backed by a Map,
     * which may change assumptions.
     *
     * @return Whether or not the underlying implementation is map-based
     */
    default boolean isMapBased() {
        return true;
    }

    /**
     * Make a copy of a map that uses the underlying storage of this map
     * factory - for example, implementations that retain only the identity
     * hash code of the keys without a reference to the original object, and
     * whose key sets do not obey the contract of java.util.Map.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param orig The original map
     * @return A map
     */
    default <T, R> Map<T, R> copyOf(Map<? extends T, ? extends R> orig) {
        boolean concur = orig instanceof ConcurrentHashMap<?, ?>
                || orig instanceof ConcurrentSkipListMap<?, ?>
                || orig instanceof ConcurrentNavigableMap<?, ?>;
        return copyOf(orig, concur);
    }

    /**
     * Make a copy of a map that uses the underlying storage of this map
     * factory, optionally choosing a concurrent or synchronized implementation
     * if <code>threadSafe</code> is specified.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param orig The original map
     * @param threadSafe If true, attempt to provide a concurrent or synchronized
     * implementation, or throw an UnsupportedOperationException if that is not
     * psossible.
     * @return A copy of the passed map
     */
    default <T, R> Map<T, R> copyOf(Map<? extends T, ? extends R> orig, boolean threadSafe) {
        Map<T, R> result = createMap(orig.size(), threadSafe);
        result.putAll(orig);
        return result;
    }
}
