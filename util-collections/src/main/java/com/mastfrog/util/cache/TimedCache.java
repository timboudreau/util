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

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A straightforward cache with expiring entries, capable of conversion into a
 * bi-directional cache. Highly concurrent, but with perfect timing it is
 * possible for a value to be computed simultaneously by two threads. It is
 * assumed that such computation is idempotent. For when you can't have a
 * dependency on Guava.
 *
 * @author Tim Boudreau
 */
public interface TimedCache<T, R, E extends Exception> {

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
    public static <T, R, E extends Exception> TimedCache<T, R, E> createThrowing(long ttl, Answerer<T, R, E> answerer) {
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
    public static <T, R> TimedCache<T, R, RuntimeException> create(long ttl, Answerer<T, R, RuntimeException> answerer) {
        return new TimedCacheImpl<>(ttl, answerer);
    }

    default <S> S ifAvailable(T key, Function<R, S> func) throws E {
        R result = get(key);
        if (result != null) {
            return func.apply(result);
        }
        return null;
    }

    default <S> S ifAvailable(T key, S defaultValue, Function<R, S> func) throws E {
        R result = get(key);
        if (result != null) {
            return func.apply(result);
        }
        return defaultValue;
    }

    /**
     * Clear this cache, removing expiring all entries immediately. If an
     * OnExpire handler has been set, it will not be called for entries that are
     * being removed (use close() if you need that).
     *
     * @return this
     */
    TimedCache<T, R, E> clear();

    /**
     * Shut down this cache, immediately expiring any entries which have not
     * been evicted.
     */
    void close();

    /**
     * Get a value from the cache, computing it if necessary.
     *
     * @param key The key to look up. May not be null.
     * @return A value or null if the answerer returned null
     * @throws E If something goes wrong
     */
    R get(T key) throws E;

    /**
     * Get a value which may be null.
     *
     * @param key The key
     * @return The value
     * @throws E If something goes wrong
     */
    Optional<R> getOptional(T key) throws E;

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
