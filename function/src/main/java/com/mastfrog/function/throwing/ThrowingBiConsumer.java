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
package com.mastfrog.function.throwing;

import com.mastfrog.function.throwing.io.IOBiConsumer;
import com.mastfrog.util.preconditions.Exceptions;
import java.util.function.BiConsumer;

/**
 * A BiConsumer which can throw.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingBiConsumer<T, R> {

    void accept(T a, R b) throws Exception;

    default ThrowingBiConsumer<T, R> andThen(ThrowingBiConsumer<T, R> tb) {
        return (a, b) -> {
            ThrowingBiConsumer.this.accept(a, b);
            tb.accept(a, b);
        };
    }

    default ThrowingBiConsumer<T, R> andThen(BiConsumer<T, R> tb) {
        return (a, b) -> {
            ThrowingBiConsumer.this.accept(a, b);
            tb.accept(a, b);
        };
    }

    default ThrowingBiConsumer<T, R> andThen(IOBiConsumer<T, R> tb) {
        return (a, b) -> {
            ThrowingBiConsumer.this.accept(a, b);
            tb.accept(a, b);
        };
    }

    /**
     * Convert to a plain BiConsumer (which will throw undeclared checked
     * exceptions).
     *
     * @return A BiConsumer
     * @deprecated Use the better-named and consistent toNonThrowing() method
     * instead
     */
    @Deprecated
    default BiConsumer<T, R> asBiconsumer() {
        return toNonThrowing();
    }

    /**
     * Convert to a plain BiConsumer (which will throw undeclared checked
     * exceptions).
     *
     * @return A BiConsumer
     */
    default BiConsumer<T, R> toNonThrowing() {
        return (T t, R u) -> {
            try {
                ThrowingBiConsumer.this.accept(t, u);
            } catch (Exception e) {
                Exceptions.chuck(e);
            }
        };
    }
}
