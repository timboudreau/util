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
package com.mastfrog.util.cache;

import com.mastfrog.abstractions.misc.MapSupplier;
import java.util.function.BiConsumer;

/**
 * A straightforward cache with expiring entries, capable of conversion into a
 * bi-directional cache. Highly concurrent, but with perfect timing it is
 * possible for a value to be computed simultaneously by two threads. It is
 * assumed that such computation is idempotent. For when you can't have a
 * dependency on Guava.
 *
 * @author Tim Boudreau
 */
public interface TimedCache<T, R, E extends Exception> extends Cache<T, R, E> {

    /**
     * Create a cache that throws a specific exception type if lookup fails.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param <E> The exception type
     * @param ttl The time after creation or last query it satisfied that a
     * value should live for, in milliseconds
     * @param answerer A function which computes the value if it is not cached
     * @return A cache
     */
    static <T, R, E extends Exception> TimedCache<T, R, E> createThrowing(long ttl,
                                                                          Answerer<T, R, E> answerer) {
        return new TimedCacheImpl<>(ttl, answerer);
    }

    /**
     * Create a cache that throws a specific exception type if lookup fails.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param <E> The exception type
     * @param ttl The time after creation or last query it satisfied that a
     * value should live for, in milliseconds
     * @param answerer A function which computes the value if it is not cached
     * @return A cache
     */
    static <T, R, E extends Exception> TimedCache<T, R, E> createThrowing(long ttl,
                                                                          Answerer<T, R, E> answerer, MapSupplier<T> backingStoreFactory) {
        return new TimedCacheImpl<>(ttl, answerer, backingStoreFactory);
    }


    /**
     * Create a cache which does not throw a checked exception on lookup.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param <E> The exception type
     * @param ttl The time after creation or last query it satisfied that a
     * value should live for, in milliseconds
     * @param answerer A function which computes the value if it is not cached
     * @return A cache
     */
    static <T, R> TimedCache<T, R, RuntimeException> create(long ttl,
                                                            Answerer<T, R, RuntimeException> answerer) {
        return new TimedCacheImpl<>(ttl, answerer);
    }
    /**
     * Create a cache which does not throw a checked exception on lookup.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param <E> The exception type
     * @param ttl The time after creation or last query it satisfied that a
     * value should live for, in milliseconds
     * @param answerer A function which computes the value if it is not cached
     * @return A cache
     */
    static <T, R> TimedCache<T, R, RuntimeException> create(long ttl, Answerer<T, R, RuntimeException> answerer, MapSupplier<T> backingStoreFactory) {
        return new TimedCacheImpl<>(ttl, answerer, backingStoreFactory);
    }

    default boolean remove(T key) {
        throw new UnsupportedOperationException("Removal not supported");
    }

    /**
     * Add a consumer which is called after a value has been expired from the
     * cache - this can be used to perform any cleanup work necessary.
     *
     * @param onExpire A biconsumer to call that receives they key and value
     * which have expired.
     *
     * @return this
     */
    TimedCache<T, R, E> onExpire(BiConsumer<T, R> onExpire);

    /**
     * Convert this cache to a bi-directional cache, providing an answerer for
     * reverse queries. The returned cache will initially share its contents
     * with this cache and show changes from the original.
     *
     * @param reverseAnswerer An answerer
     * @return A bidirectional cache with the same contents as this one
     */
    TimedBidiCache<T, R, E> toBidiCache(Answerer<R, T, E> reverseAnswerer);
}
