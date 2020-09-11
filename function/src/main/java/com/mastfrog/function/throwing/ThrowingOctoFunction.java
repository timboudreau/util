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

import java.util.Objects;
import java.util.function.Function;

/**
 * Like a BiFunction, but taking eight arguments.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingOctoFunction<A, B, C, D, E, F, G, H, R> {

    R apply(A a, B b, C c, D d, E e, F f, G g, H h) throws Exception;

    default <M> ThrowingOctoFunction<A, B, C, D, E, F, G, H, M> andThen(Function<? super R, ? extends M> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c, D d, E e, F f, G g, H h) -> after.apply(apply(a, b, c, d, e, f, g, h));
    }
}
