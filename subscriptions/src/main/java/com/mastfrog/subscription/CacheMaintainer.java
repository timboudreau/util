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
package com.mastfrog.subscription;

import com.mastfrog.util.cache.MapCache;
import com.mastfrog.util.collections.MapFactory;
import java.util.function.Function;

/**
 * Provided when you build a Subscribable - typically it is needed to maintain
 * some objects involved in processing events, associated with one key; the
 * subscribable automatically manages deleting the cache entries for keys that
 * have been removed from it, so cache entries have the same lifecycle as the
 * objects they're subscribed to.
 *
 * @author Tim Boudreau
 */
public interface CacheMaintainer<K> {

    /**
     * Create a cache whose contents will be updated or cleaned up when subscribers to the
     * associated Subscribable are removed.
     *
     * @param <T>
     * @param valueType The base type for the cache (should be the exact type you want;
     * we use the supertype so you can use generified types)
     * @param type The map factory (determines cached object longevity)
     * @param valueSupplier A function to compute values for the cache
     * @return The cache
     */
    <T> MapCache<K, T> createCache(Class<? super T> valueType, MapFactory type, Function<K,T> valueSupplier);
}
