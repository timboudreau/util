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

import com.mastfrog.util.preconditions.Checks;
import com.mastfrog.util.preconditions.Exceptions;
import java.util.Objects;
import java.util.function.Function;

/**
 * A function which can throw exceptions.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingFunction<In, Out> {

    Out apply(In arg) throws Exception;

    default Function<In, Out> toFunction() {
        return (In t) -> {
            try {
                return ThrowingFunction.this.apply(t);
            } catch (Exception ex) {
                return Exceptions.chuck(ex);
            }
        };
    }

    default <V> ThrowingFunction<V, Out> compose(Function<? super V, ? extends In> before) {
        Checks.notNull("before", before);
        return (V v) -> apply(before.apply(v));
    }

    default <V> ThrowingFunction<In, V> andThen(Function<? super Out, ? extends V> after) {
        Checks.notNull("after", after);
        Objects.requireNonNull(after);
        return (In t) -> after.apply(apply(t));
    }

    default <NextOut> ThrowingFunction<In, NextOut> andThen(ThrowingFunction<Out, NextOut> f) {
        return in -> {
            return f.apply(ThrowingFunction.this.apply(in));
        };
    }
}
