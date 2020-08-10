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
