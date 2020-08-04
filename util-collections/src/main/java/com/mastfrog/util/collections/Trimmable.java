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
package com.mastfrog.util.collections;

import java.util.Map;
import java.util.Set;

/**
 * Interface for array-based collections which may over-allocate memory for
 * performance, or which are treated as immutable once populated and will
 * not need any additional preallocated slots for items.
 *
 * @author Tim Boudreau
 */
public interface Trimmable {

    /**
     * Discard any large data structures not currently needed by the object;
     * note that this may involve copying large arrays, and should be called
     * infrequently or when the data structure has reached a steady state where
     * future modifications are unlikely or far in the future.
     */
    void trim();

    /**
     * Subinterface that implements Set.
     *
     * @param <T> The type
     */
    public interface TrimmableSet<T> extends Set<T>, Trimmable {

    }

    public interface TrimmableMap<K,V> extends Map<K, V>, Trimmable {

    }
}
