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

import java.util.Arrays;

/**
 *
 * @author Tim Boudreau
 */
class ArrayPredicate implements EnhIntPredicate {

    final int[] vals;
    final boolean negated;

    ArrayPredicate(int[] vals, boolean negated) {
        this.vals = vals;
        this.negated = negated;
    }

    @Override
    public boolean test(int val) {
        boolean result;
        if (vals.length == 1) {
            result = vals[0] == val;
        } else {
            result = Arrays.binarySearch(vals, val) >= 0;
        }
        return negated ? !result : result;
    }

    @Override
    public String toString() {
        if (vals.length == 1) {
            return (negated ? "!=" : "==") + vals[0];
        } else {
            return (negated ? "noneOf(" : "anyOf") + Arrays.toString(vals) + ")";
        }
    }

    @Override
    public EnhIntPredicate negate() {
        return new ArrayPredicate(vals, !negated);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Arrays.hashCode(this.vals);
        hash = 59 * hash + (this.negated ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof ArrayPredicate || obj instanceof ArrayPredicateWithNames) {
            ArrayPredicate other = (ArrayPredicate) obj;
            if (this.negated != other.negated) {
                return false;
            }
            return Arrays.equals(this.vals, other.vals);
        } else if (obj instanceof SinglePredicate && vals.length == 1) {
            SinglePredicate sp = (SinglePredicate) obj;
            return sp.val == vals[0] && sp.negated == negated;
        }
        return false;
    }

}
