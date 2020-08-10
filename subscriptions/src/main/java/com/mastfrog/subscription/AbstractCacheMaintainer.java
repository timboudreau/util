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

import com.mastfrog.util.collections.MapFactories;
import com.mastfrog.util.cache.MapCache;
import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.collections.MapFactory;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author Tim Boudreau
 */
public abstract class AbstractCacheMaintainer<K> implements CacheMaintainer<K> {

    protected Map<Class<?>, MapCache<K, ?>> caches
            = CollectionUtils.weakValueMap(MapFactories.EQUALITY, 16, WeakReference::new);

    @Override
    @SuppressWarnings("unchecked")
    public <T> MapCache<K, T> createCache(Class<? super T> valueType, MapFactory type, Function<K, T> valueSupplier) {
        if (caches.containsKey(valueType)) {
            throw new IllegalStateException("Already have a cache for " + valueType.getName());
        }
        MapCache<K, T> result = (MapCache<K, T>) caches.get(valueType);
        if (result == null) {
            result = new MapCache<>(type.createMap(16, true), valueSupplier);
            caches.put(valueType, result);
        }
        return result;
    }

    protected void onKeyDestroyed(K key) {
        for (Map.Entry<Class<?>, MapCache<K, ?>> e : caches.entrySet()) {
            e.getValue().remove(key);
        }
    }
}
