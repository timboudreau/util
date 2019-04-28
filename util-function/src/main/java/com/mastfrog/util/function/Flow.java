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
package com.mastfrog.util.function;

import com.mastfrog.function.EnhIntSupplier;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import com.mastfrog.function.ToIntBiFunction;

/**
 *
 * @author Tim Boudreau
 */
public final class Flow {

    public static void ifTrue(boolean val, Runnable run) {
        if (val) {
            run.run();
        }
    }

    public static void ifTrue(BooleanSupplier supp, Runnable run) {
        if (supp.getAsBoolean()) {
            run.run();
        }
    }

    public static void ifElse(BooleanSupplier supp, Runnable ifTrue, Runnable ifFalse) {
        if (supp.getAsBoolean()) {
            ifTrue.run();
        } else {
            ifFalse.run();
        }
    }

    public static void ifNull(Object val, Runnable run) {
        if (val == null) {
            run.run();
        }
    }

    public static void ifNotNull(Object val, Runnable run) {
        if (val != null) {
            run.run();
        }
    }

    public static <T> void ifEqual(Object a, Object b, Runnable run) {
        if (Objects.equals(a, b)) {
            run.run();
        }
    }

    public static <T> void ifUnequal(Object a, Object b, Runnable run) {
        if (!Objects.equals(a, b)) {
            run.run();
        }
    }

    public static <T, R> ToIntBiFunction<T> comparison(ToIntBiFunction<T> bif1, Supplier<R> a2, Supplier<R> b2, ToIntBiFunction<R> bif2) {
        return bif1.<R>ifZero(a2, b2, bif2);
    }

    public static <T extends Comparable<T>, R extends Comparable<R>> EnhIntSupplier comparison(Supplier<T> a1, Supplier<T> b1, Supplier<R> a2, Supplier<R> b2) {
        return ToIntBiFunction.comparer(a1, b1).ifZero(ToIntBiFunction.comparer(a2, b2));
    }

    public static <T extends Comparable<T>, R extends Comparable<R>> EnhIntSupplier comparison(T a1, T b1, R a2, R b2) {
        return ToIntBiFunction.comparer(a1, b1).ifZero(ToIntBiFunction.comparer(a2, b2));
    }

    public static <T extends Comparable<T>, R extends Comparable<R>, S extends Comparable<S>> EnhIntSupplier comparison(Supplier<T> a1, Supplier<T> b1, Supplier<R> a2, Supplier<R> b2, Supplier<S> a3, Supplier<S> b3) {
        return ToIntBiFunction.comparer(a1, b1).ifZero(ToIntBiFunction.comparer(a2, b2)).ifZero(ToIntBiFunction.comparer(a3, b3));
    }

    public static <T extends Comparable<T>, R extends Comparable<R>, S extends Comparable<S>, Q extends Comparable<Q>> EnhIntSupplier comparison(Supplier<T> a1, Supplier<T> b1, Supplier<R> a2, Supplier<R> b2, Supplier<S> a3, Supplier<S> b3, Supplier<Q> a4, Supplier<Q> b4) {
        return ToIntBiFunction.comparer(a1, b1).ifZero(ToIntBiFunction.comparer(a2, b2)).ifZero(ToIntBiFunction.comparer(a3, b3)).ifZero(ToIntBiFunction.comparer(a4, b4));
    }

    public static <T extends Comparable<T>, R extends Comparable<R>, S extends Comparable<S>, Q extends Comparable<Q>, P extends Comparable<P>> EnhIntSupplier comparison(Supplier<T> a1, Supplier<T> b1, Supplier<R> a2, Supplier<R> b2, Supplier<S> a3, Supplier<S> b3, Supplier<Q> a4, Supplier<Q> b4, Supplier<P> a5, Supplier<P> b5) {
        return ToIntBiFunction.comparer(a1, b1).ifZero(ToIntBiFunction.comparer(a2, b2)).ifZero(ToIntBiFunction.comparer(a3, b3)).ifZero(ToIntBiFunction.comparer(a4, b4))
                .ifZero(ToIntBiFunction.comparer(a5, b5));
    }

    private Flow() {
        throw new AssertionError();
    }
}
