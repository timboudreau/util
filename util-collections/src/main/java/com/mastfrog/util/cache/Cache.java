/*
 * The MIT License
 *
 * Copyright 2019 Tim Boudreau.
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
import java.util.function.Function;

/**
 * Basic abstraction for a cache.
 *
 * @author Tim Boudreau
 */
public interface Cache<T, R, E extends Exception> extends AutoCloseable {

    /**
     * Clear this cache, removing expiring all entries immediately. If an
     * OnExpire handler has been set, it will not be called for entries that are
     * being removed (use close() if you need that).
     *
     * @return this
     */
    Cache<T, R, E> clear();

    /**
     * Shut down this cache, immediately expiring any entries which have not
     * been evicted if this is a cache entries expire from.
     */
    @Override
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
     * Get an object if it is available, using the passed function to
     * convert the result if it is non-null.
     *
     * @param <S> A type
     * @param key The key
     * @param func A conversion function
     * @return A value or null
     * @throws E If something goes wrong
     */
    default <S> S ifAvailable(T key, Function<R, S> func) throws E {
        R result = get(key);
        if (result != null) {
            return func.apply(result);
        }
        return null;
    }

    /**
     * Get an object if it is available, using the passed function to
     * convert the result if it is non-null, otherwise returning the
     * passed default value.
     *
     * @param <S> A type
     * @param key The key
     * @param defaultValue A default value to use if nothing is cached
     * @param func A conversion function
     * @return The cached value or the default value
     * @throws E If something goes wrong
     */
    default <S> S ifAvailable(T key, S defaultValue, Function<R, S> func) throws E {
        R result = get(key);
        if (result != null) {
            return func.apply(result);
        }
        return defaultValue;
    }
}
