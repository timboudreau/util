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

import java.util.function.IntPredicate;

/**
 *
 * @author Tim Boudreau
 */
final class FixedIntPredicate implements EnhIntPredicate {

    static final EnhIntPredicate INT_FALSE, INT_TRUE;
    static {
        INT_FALSE = new FixedIntPredicate(false);
        INT_TRUE = new FixedIntPredicate(true);
    }
    private final boolean value;

    FixedIntPredicate(boolean value) {
        this.value = value;
    }

    @Override
    public boolean test(int val) {
        return value;
    }

    @Override
    public EnhIntPredicate and(IntPredicate other) {
        return value ? EnhIntPredicate.of(other) : this;
    }

    @Override
    public EnhIntPredicate negate() {
        return this == INT_TRUE ? INT_FALSE : INT_TRUE;
    }

    @Override
    public EnhIntPredicate or(IntPredicate other) {
        return value ? this : EnhIntPredicate.of(other);
    }

    @Override
    @SuppressWarnings(value = "EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        return (this == INT_FALSE && o == INT_FALSE)
                || (this == INT_TRUE && o == INT_TRUE);
    }

    @Override
    public int hashCode() {
        return hashCode(value);
    }

    @Override
    public String toString() {
        return stringValue(value);
    }

    static String stringValue(boolean value) {
        return value ? "alwaysTrue" : "alwaysFalse";
    }

    static int hashCode(boolean val) {
        return val ? 103 : 7;
    }
}
