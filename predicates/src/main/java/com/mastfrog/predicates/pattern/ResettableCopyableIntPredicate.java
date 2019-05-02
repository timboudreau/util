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
package com.mastfrog.predicates.pattern;

import com.mastfrog.abstractions.Copyable;
import com.mastfrog.abstractions.Resettable;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 *
 * @author Tim Boudreau
 */
public interface ResettableCopyableIntPredicate<T extends ResettableCopyableIntPredicate<T>> extends IntPredicate, Resettable, Copyable<T> {

    @Override
    ResettableCopyableIntPredicate<?> or(IntPredicate other);

    @Override
    ResettableCopyableIntPredicate<?> and(IntPredicate other);

    @Override
    ResettableCopyableIntPredicate<?> negate();

    /**
     * Convert this into an Object-taking predicate which uses the passed
     * function to resolve an integer.
     *
     * @param <R> The type the returned predicate takes
     * @param func A conversion function
     * @return A predicate
     */
    default <R> Predicate<R> convertedBy(ToIntFunction<R> func) {
        return new Predicate<R>() {
            public boolean test(R val) {
                return ResettableCopyableIntPredicate.this.test(func.applyAsInt(val));
            }

            public String toString() {
                return "convert(" + ResettableCopyableIntPredicate.this + " <- " + func + ")";
            }
        };
    }
}
