/*
 * The MIT License
 *
 * Copyright 2015 Tim Boudreau.
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
package com.mastfrog.util;

/**
 * Stub implementation in order to support JDK 7.
 *
 * @author Tim Boudreau
 */
public final class Optional<T> {

    private final T value;

    Optional(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public static <T> Optional<T> of(T obj) {
        return new Optional<T>(obj);
    }

    public static <T> Optional<T> fromNullable(T obj) {
        return ofNullable(obj);
    }

    public static <T> Optional<T> empty() {
        return new Optional<T>(null);
    }

    public static <T> Optional<T> ofNullable(T obj) {
        return new Optional<T>(obj);
    }

    public boolean isPresent() {
        return value != null;
    }

    public int hashCode() {
        return value == null ? 0 : value.hashCode();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o instanceof Optional<?>) {
            boolean result = (value == null) == (((Optional<?>) o).value == null);
            if (result && value != null) {
                return value.equals(((Optional<?>) o).value);
            }
        }
        return false;
    }
}
