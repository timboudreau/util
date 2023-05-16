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

import com.mastfrog.util.preconditions.Exceptions;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A BiFunction which can throw.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingBiFunction<InA, InB, Out> {

    Out apply(InA t, InB u) throws Exception;

    default <NextOut> ThrowingBiFunction<InA, InB, NextOut> andThen(Function<? super Out, ? extends NextOut> after) {
        Objects.requireNonNull(after);
        return (InA t, InB u) -> after.apply(apply(t, u));
    }

    /**
     * Convert to a non-throwing equivalent. Note that the resulting method
     * <i>will</i> rethrow any thrown checked exceptions.
     *
     * @return An equivalent function that does not declare the exceptions which
     * it throws
     */
    default BiFunction<InA, InB, Out> toNonThrowing() {
        return (a, b) -> {
            try {
                return apply(a, b);
            } catch (Exception ex) {
                return Exceptions.chuck(ex);
            }
        };
    }
}
