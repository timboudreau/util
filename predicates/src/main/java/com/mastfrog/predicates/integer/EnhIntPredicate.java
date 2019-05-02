/*
 * The MIT License
 *
 * Copyright 2019 Tim Boudreau.
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
package com.mastfrog.predicates.integer;

import com.mastfrog.abstractions.Named;
import com.mastfrog.predicates.NamedPredicate;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 *
 * @author Tim Boudreau
 */
public interface EnhIntPredicate extends IntPredicate {

    /**
     * Create a predicate which will only match the first <i>n</i>
     * matches it encounters.
     *
     * @param max The number of matches to return true for
     * @return A predicate
     */
    default EnhIntPredicate firstNmatches(int max) {
        return new EnhIntPredicate() {
            private int count;

            @Override
            public boolean test(int value) {
                boolean result = EnhIntPredicate.this.test(value);
                if (result && count++ > max) {
                    result = false;
                }
                return result;
            }

            @Override
            public String toString() {
                return "first-" + max + "-matches-of-" + EnhIntPredicate.this;
            }
        };
    }

    /**
     * Wraps this predicate in one which will ignore <i>n</i> matches before
     * returning true, resetting once it does so and counting to <i>n</i>
     * matches again before returning true.
     *
     * @param n The number of matches to ignore
     * @return A predicate
     */
    default EnhIntPredicate ignoringMatches(int n) {
        return new EnhIntPredicate() {
            private int currentCount;

            @Override
            public boolean test(int value) {
                boolean result = EnhIntPredicate.this.test(value);
                if (result && currentCount++ < n) {
                    return false;
                }
                if (result) {
                    currentCount = 0;
                }
                return result;
            }

            @Override
            public String toString() {
                return "ignore-first-" + n + "-matches-of-" + EnhIntPredicate.this;
            }
        };
    }

    @Override
    default EnhIntPredicate or(IntPredicate other) {
        return new EnhIntPredicate() {
            @Override
            public boolean test(int value) {
                return EnhIntPredicate.this.test(value) || other.test(value);
            }

            @Override
            public String toString() {
                return EnhIntPredicate.this.toString() + " || " + other.toString();
            }
        };
    }

    @Override
    default EnhIntPredicate and(IntPredicate other) {
        return new EnhIntPredicate() {
            @Override
            public boolean test(int value) {
                return EnhIntPredicate.this.test(value) && other.test(value);
            }

            @Override
            public String toString() {
                return EnhIntPredicate.this.toString() + " & " + other.toString();
            }
        };
    }

    @Override
    default EnhIntPredicate negate() {
        return new EnhIntPredicate() {
            @Override
            public EnhIntPredicate negate() {
                return EnhIntPredicate.this;
            }

            @Override
            public boolean test(int value) {
                return !EnhIntPredicate.this.test(value);
            }

            @Override
            public String toString() {
                return "!(" + EnhIntPredicate.this + ")";
            }
        };
    }

    static EnhIntPredicate of(IntPredicate predicate) {
        if (predicate instanceof EnhIntPredicate) {
            return (EnhIntPredicate) predicate;
        } else {
            return new EnhIntPredicate() {
                @Override
                public boolean test(int value) {
                    return predicate.test(value);
                }

                @Override
                public String toString() {
                    return Named.findName(predicate.toString());
                }
            };
        }
    }

    default <R> Predicate<R> convertedBy(ToIntFunction<R> func) {
        return new NamedPredicate<R>() {
            public boolean test(R val) {
                return EnhIntPredicate.this.test(func.applyAsInt(val));
            }

            @Override
            public String toString() {
                return "convert(" + EnhIntPredicate.this + " <- " + func + ")";
            }

            @Override
            public String name() {
                return toString();
            }
        };
    }

}
