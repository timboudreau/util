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
 * Eight argument predicate.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface OctoPredicate<A, B, C, D, E, F, G, H> {

    boolean test(A a, B b, C c, D d, E e, F f, G g, H h);

    default OctoPredicate<A, B, C, D, E, F, G, H> and(OctoPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F, ? super G, ? super H> other) {
        return (a, b, c, d, e, f, g, h) -> {
            return test(a, b, c, d, e, f, g, h) && other.test(a, b, c, d, e, f, g, h);
        };
    }

    default OctoPredicate<A, B, C, D, E, F, G, H> or(OctoPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F, ? super G, ? super H> other) {
        return (a, b, c, d, e, f, g, h) -> {
            return test(a, b, c, d, e, f, g, h) || other.test(a, b, c, d, e, f, g, h);
        };
    }

    default OctoPredicate<A, B, C, D, E, F, G, H> xor(OctoPredicate<? super A, ? super B, ? super C, ? super D, ? super E, ? super F, ? super G, ? super H> other) {
        return (a, b, c, d, e, f, g, h) -> {
            return test(a, b, c, d, e, f, g, h) != other.test(a, b, c, d, e, f, g, h);
        };
    }
}
