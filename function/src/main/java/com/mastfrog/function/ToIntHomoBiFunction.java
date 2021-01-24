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
package com.mastfrog.function;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;

/**
 * A BiFunction returning a primitive int. Extends Comparator for convenience,
 * since composing these is a straightforward way to build comparators with
 * composed conditions.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ToIntHomoBiFunction<T> extends Comparator<T>, ToIntBiFunction<T,T> {

    @Override
    int applyAsInt(T a, T b);

    static <R extends Comparable<R>> EnhIntSupplier comparer(Supplier<R> a, Supplier<R> b) {
        return () -> a.get().compareTo(b.get());
    }

    static <R extends Comparable<R>> EnhIntSupplier comparer(R a, R b) {
        return () -> a.compareTo(b);
    }

    static <R extends Comparable<R>> ToIntHomoBiFunction<R> comparison() {
        return Comparable::compareTo;
    }

    default <R> ToIntHomoBiFunction<R> transform(Function<R, T> func) {
        return (R a, R b) -> ToIntHomoBiFunction.this.applyAsInt(func.apply(a), func.apply(b));
    }

    default <R> ToIntHomoBiFunction<T> ifZero(R ra, R rb, ToIntHomoBiFunction<R> next) {
        return or((int value) -> value != 0, ra, rb, next);
    }

    default <R> ToIntHomoBiFunction<T> ifZero(Supplier<R> ra, Supplier<R> rb, ToIntHomoBiFunction<R> next) {
        return or((int value) -> value != 0, ra, rb, next);
    }

    default <R> ToIntHomoBiFunction<T> or(IntPredicate test, R ra, R rb, ToIntHomoBiFunction<R> next) {
        return (T a, T b) -> {
            int result = ToIntHomoBiFunction.this.applyAsInt(a, b);
            if (!test.test(result)) {
                return next.applyAsInt(ra, rb);
            }
            return result;
        };
    }

    default <R> ToIntHomoBiFunction<T> or(IntPredicate test, Supplier<R> ra, Supplier<R> rb, ToIntHomoBiFunction<R> next) {
        return (T a, T b) -> {
            int result = ToIntHomoBiFunction.this.applyAsInt(a, b);
            if (!test.test(result)) {
                return next.applyAsInt(ra.get(), rb.get());
            }
            return result;
        };
    }

    default EnhIntSupplier toIntSupplier(Supplier<T> a, Supplier<T> b) {
        return () -> ToIntHomoBiFunction.this.applyAsInt(a.get(), b.get());
    }

    default EnhIntSupplier toIntSupplier(T a, T b) {
        return () -> ToIntHomoBiFunction.this.applyAsInt(a, b);
    }

    @Override
    public default int compare(T o1, T o2) {
        return ToIntHomoBiFunction.this.applyAsInt(o1, o2);
    }
}
