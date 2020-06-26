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
package com.mastfrog.function;

/**
 * Seven argument predicate.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface SeptaPredicate<A, B, C, D, E, F, G> {

    boolean test(A a, B b, C c, D d, E e, F f, G g);

    default SeptaPredicate<A, B, C, D, E, F, G> and(SeptaPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F,? super G> other) {
        return (a, b, c, d, e, f, g) -> {
            return test(a, b, c, d, e, f, g) && other.test(a, b, c, d, e, f, g);
        };
    }

    default SeptaPredicate<A, B, C, D, E, F, G> or(SeptaPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F, ? super G> other) {
        return (a, b, c, d, e, f, g) -> {
            return test(a, b, c, d, e, f, g) || other.test(a, b, c, d, e, f, g);
        };
    }

    default SeptaPredicate<A, B, C, D, E, F, G> xor(SeptaPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F, ? super G> other) {
        return (a, b, c, d, e, f, g) -> {
            return test(a, b, c, d, e, f, g) != other.test(a, b, c, d, e, f, g);
        };
    }
}
