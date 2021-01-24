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
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;

/**
 * A predicate which takes two primitive longs.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface LongBiPredicate {

    static LongBiPredicate NEVER = (a, b) -> false;
    static LongBiPredicate ALWAYS = (a, b) -> true;

    boolean test(long a, long b);

    default BooleanSupplier toBooleanSupplier(LongSupplier a, LongSupplier b) {
        return () -> {
            return test(a.getAsLong(), b.getAsLong());
        };
    }

    default LongBiPredicate negate() {
        return (a, b) -> {
            return !test(a, b);
        };
    }

    default LongBiPredicate or(LongBiPredicate other) {
        return (a, b) -> {
            return test(a, b) || other.test(a, b);
        };
    }

    default LongBiPredicate and(LongBiPredicate other) {
        return (a, b) -> {
            return test(a, b) && other.test(a, b);
        };
    }

    default LongBiPredicate andNot(LongBiPredicate other) {
        return and(other.negate());
    }

    default LongBiPredicate xor(LongBiPredicate other) {
        return (a, b) -> {
            return test(a, b) != other.test(a, b);
        };
    }

    default IntBiPredicate toIntBiPredicate() {
        return (a, b) -> {
            return this.test(a, b);
        };
    }

    static LongBiPredicate fromPredicates(LongPredicate aPredicate, LongPredicate bPredicate) {
        return new LongBiPredicate() { // Use an inner class for loggability
            @Override
            public boolean test(long a, long b) {
                return aPredicate.test(a) && bPredicate.test(b);
            }

            @Override
            public String toString() {
                return "{" + aPredicate + ", " + bPredicate + "}";
            }
        };
    }
}
