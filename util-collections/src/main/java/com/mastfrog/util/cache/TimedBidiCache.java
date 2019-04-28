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
import java.util.function.Function;

/**
 * A bi-directional variant of TimedCache, which allows querying for keys or
 * values, assuming either can be constructed from the other.
 *
 * @param <T> The key type
 * @param <R> The value type
 * @param <E> The exception that may be thrown by the function which looks
 * things up
 */
public interface TimedBidiCache<T, R, E extends Exception> extends TimedCache<T, R, E> {

    /**
     * Get the key for a value.
     *
     * @param value The value
     * @return The key, or null
     * @throws E If something goes wrong
     */
    T getKey(R value) throws E;

    /**
     * Get the key for a value, wrapped in an Optional.
     *
     * @param value The value
     * @return The key, which will be present if contained in the cache already
     * or if the reverse anwerer returned non-null
     * @throws E
     */
    Optional<T> getKeyOptional(R value) throws E;

    default <S> S ifKeyAvailable(R value, Function<T, S> func) throws E {
        T result = getKey(value);
        if (result != null) {
            return func.apply(result);
        }
        return null;
    }

    default <S> S ifKeyAvailable(R value, S defaultValue, Function<T, S> func) throws E {
        T result = getKey(value);
        if (result != null) {
            return func.apply(result);
        }
        return defaultValue;
    }
}
