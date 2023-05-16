/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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

import com.mastfrog.function.LongBiConsumer;
import com.mastfrog.util.preconditions.Exceptions;

/**
 * A consumer which takes two primitive longs and throws.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingLongBiConsumer {

    void accept(long a, long b) throws Exception;

    default ThrowingIntBiConsumer toIntBiConsumer() {
        return (a, b) -> accept(a, b);
    }

    /**
     * Returns a wrapper which does not declare the checked exception (but will
     * rethrow it if thrown via Exceptions.chuck()).
     *
     * @return A wrapper around this function
     */
    default LongBiConsumer toNonThrowing() {
        return (a, b) -> {
            try {
                accept(a, b);
            } catch (Exception ex) {
                Exceptions.chuck(ex);
            }
        };
    }
}
