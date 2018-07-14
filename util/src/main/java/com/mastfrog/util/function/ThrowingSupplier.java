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
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Supplier interface which can throw exceptions.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingSupplier<T> {

    T get() throws Exception;

    /**
     * Get a view of this object as a Java supplier (which can throw undeclared
     * exception).
     *
     * @return A supplier
     */
    default Supplier<T> asSupplier() {
        return () -> {
            try {
                return ThrowingSupplier.this.get();
            } catch (Exception e) {
                return Exceptions.chuck(e);
            }
        };
    }

    default <R> ThrowingSupplier<R> adapt(Function<T, R> func) {
        return () -> {
            return func.apply(ThrowingSupplier.this.get());
        };
    }
}
