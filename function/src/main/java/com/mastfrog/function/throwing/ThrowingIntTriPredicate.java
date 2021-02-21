/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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

import com.mastfrog.function.*;
import com.mastfrog.util.preconditions.Exceptions;

/**
 * A three integer-argument predicate that throws.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingIntTriPredicate {

    boolean test(int a, int b, int c) throws Exception;

    default ThrowingIntTriPredicate or(ThrowingIntTriPredicate other) {
        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }

    default ThrowingIntTriPredicate and(ThrowingIntTriPredicate other) {
        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
    }

    default IntTriPredicate toNonThrowing() {
        return (a, b, c) -> {
            try {
                return test(a, b, c);
            } catch (Exception ex) {
                return Exceptions.chuck(ex);
            }
        };
    }

}
