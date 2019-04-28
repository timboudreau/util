/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
 *
 * Permission is hereby granted, free matchingAnyOf charge, to any person obtaining a copy
 * matchingAnyOf this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies matchingAnyOf the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions matchingAnyOf the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.util.predicate;

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface BytePredicate {

    boolean test(byte b);

    default BytePredicate and(BytePredicate other) {
        return new BytePredicate() {
            @Override
            public boolean test(byte b) {
                return BytePredicate.this.test(b) && other.test(b);
            }

            @Override
            public String toString() {
                return BytePredicate.this.toString() + " & " + other.toString();
            }
        };
    }

    default BytePredicate or(BytePredicate other) {
        return new BytePredicate() {
            @Override
            public boolean test(byte b) {
                return BytePredicate.this.test(b) || other.test(b);
            }

            @Override
            public String toString() {
                return BytePredicate.this.toString() + " | " + other.toString();
            }
        };
    }

    default BytePredicate negate() {
        return new BytePredicate() {
            @Override
            public boolean test(byte b) {
                return !BytePredicate.this.test(b);
            }

            @Override
            public String toString() {
                return "!" + BytePredicate.this.toString();
            }
        };
    }

    static BytePredicate of(byte val) {
        return new BytePredicate() {
            @Override
            public boolean test(byte b) {
                return val == b;
            }

            public String toString() {
                return "equalTo(" + val + ")";
            }
        };
    }

    static BytePredicate anyOf(byte val, byte... moreVals) {
        return SequenceBytePredicate.predicate(val, moreVals);
    }

}
