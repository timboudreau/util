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
