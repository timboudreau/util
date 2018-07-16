/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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
package com.mastfrog.util.function;

import com.mastfrog.util.preconditions.Exceptions;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingConsumer<T> {

    void apply(T obj) throws Exception;

    /**
     * Get a view of this object as a Java consumer (which can throw undeclared
     * exception).
     *
     * @return A consumer
     */
    default Consumer<T> asConsumer() {
        return (obj) -> {
            try {
                ThrowingConsumer.this.apply(obj);
            } catch (Exception ex) {
                Exceptions.chuck(ex);
            }
        };
    }

    default <R> ThrowingConsumer<R> adapt(Function<R, T> conversion) {
        return (R r) -> {
            ThrowingConsumer.this.apply(conversion.apply(r));
        };
    }

    default ThrowingConsumer<T> then(ThrowingConsumer<T> next) {
        return (T obj) -> {
            ThrowingConsumer.this.apply(obj);
            next.apply(obj);
        };
    }
}
