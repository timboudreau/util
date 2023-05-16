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

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;

/**
 * A predicate which takes two primitive integers.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IntBiPredicate {

    static IntBiPredicate NEVER = (a, b) -> false;
    static IntBiPredicate ALWAYS = (a, b) -> true;

    boolean test(int a, int b);

    default BooleanSupplier toBooleanSupplier(IntSupplier a, IntSupplier b) {
        return () -> {
            return test(a.getAsInt(), b.getAsInt());
        };
    }

    default IntBiPredicate negate() {
        return (a, b) -> {
            return !test(a, b);
        };
    }

    default IntBiPredicate or(IntBiPredicate other) {
        return (a, b) -> {
            return test(a, b) || other.test(a, b);
        };
    }

    default IntBiPredicate and(IntBiPredicate other) {
        return (a, b) -> {
            return test(a, b) && other.test(a, b);
        };
    }

    default IntBiPredicate xor(IntBiPredicate other) {
        return (a, b) -> {
            return test(a, b) != other.test(a, b);
        };
    }

    default IntBiPredicate andNot(IntBiPredicate other) {
        return and(other.negate());
    }

    default LongBiPredicate toLongBiPredicate() {
        return (a, b) -> {
            if (a < Integer.MIN_VALUE || a > Integer.MAX_VALUE || b < Integer.MIN_VALUE || b > Integer.MAX_VALUE) {
                return false;
            }
            return test((int) a, (int) b);
        };
    }

    static IntBiPredicate fromPredicates(IntPredicate aPredicate, IntPredicate bPredicate) {
        return new IntBiPredicate() {
            @Override
            public boolean test(int a, int b) {
                return aPredicate.test(a) && bPredicate.test(b);
            }

            @Override
            public String toString() {
                return "{" + aPredicate + ", " + bPredicate + "}";
            }
        };
    }

    default ShortBiPredicate toShortBiPredicate() {
        return (a, b) -> test(a, b);
    }
}
