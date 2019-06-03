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
package com.mastfrog.function;

import java.util.Arrays;
import java.util.function.IntPredicate;

/**
 * A predicate for testing bytes.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface BytePredicate {

    boolean test(byte value);

    static BytePredicate fromSignedIntPredicate(IntPredicate pred) {
        return new BytePredicate() {
            @Override
            public boolean test(byte value) {
                return pred.test(value & 0xFF);
            }

            public String toString() {
                return "signedInt(" + pred + ")";
            }
        };
    }

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

    public static BytePredicate of(byte val) {
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

    public static BytePredicate anyOf(byte val, byte... moreVals) {
        Arrays.sort(moreVals);
        return new BytePredicate() {
            @Override
            public boolean test(byte value) {
                return value == val || Arrays.binarySearch(moreVals, value) >= 0;
            }

            public String toString() {
                StringBuilder sb = new StringBuilder().append("anyOf(").append(val);
                if (moreVals.length > 0) {
                    sb.append(",");
                }
                for (int i = 0; i < moreVals.length; i++) {
                    sb.append(moreVals[i]);
                    if (i < moreVals.length - 1) {
                        sb.append(',');
                    }
                }
                return sb.append(')').toString();
            }
        };
    }

    public static BytePredicate noneOf(byte val, byte... moreVals) {
        Arrays.sort(moreVals);
        return new BytePredicate() {
            @Override
            public boolean test(byte value) {
                return value != val && Arrays.binarySearch(moreVals, value) < 0;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder("noneOf(").append(val);
                if (moreVals.length > 0) {
                    sb.append(",");
                }
                for (int i = 0; i < moreVals.length; i++) {
                    sb.append(moreVals[i]);
                    if (i < moreVals.length - 1) {
                        sb.append(',');
                    }
                }
                return sb.append(')').toString();
            }
        };
    }
}
