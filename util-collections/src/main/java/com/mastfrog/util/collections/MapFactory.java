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

    default boolean isMapBased() {
        return true;
    }

    default <T, R> Map<T, R> copyOf(Map<? extends T, ? extends R> orig) {
        boolean concur = orig instanceof ConcurrentHashMap<?, ?>
                || orig instanceof ConcurrentSkipListMap<?, ?>
                || orig instanceof ConcurrentNavigableMap<?, ?>;
        return copyOf(orig, concur);
    }

    default <T, R> Map<T, R> copyOf(Map<? extends T, ? extends R> orig, boolean threadSafe) {
        Map<T, R> result = createMap(orig.size(), threadSafe);
        result.putAll(orig);
        return result;
    }
}
