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

import com.mastfrog.function.OctoConsumer;

/**
 * A BiConsumer which can throw.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingOctoConsumer<A, B, C, D, E, F, G, H> {

    void accept(A a, B b, C c, D d, E e, F f, G g, H h) throws Exception;

    default ThrowingOctoConsumer<A, B, C, D, E, F, G, H> andThen(ThrowingOctoConsumer<A, B, C, D, E, F, G, H> tb) {
        return (a, b, c, d, e, f, g, h) -> {
            ThrowingOctoConsumer.this.accept(a, b, c, d, e, f, g, h);
            tb.accept(a, b, c, d, e, f, g, h);
        };
    }

    default ThrowingOctoConsumer<A, B, C, D, E, F, G, H> andThen(OctoConsumer<A, B, C, D, E, F, G, H> tb) {
        return (a, b, c, d, e, f, g, h) -> {
            ThrowingOctoConsumer.this.accept(a, b, c, d, e, f, g, h);
            tb.accept(a, b, c, d, e, f, g, h);
        };
    }
}
