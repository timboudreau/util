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
import java.util.function.IntPredicate;

/**
 *
 * @author Tim Boudreau
 */
class SinglePredicate implements EnhIntPredicate {

    final boolean negated;
    final int val;

    SinglePredicate(boolean negated, int val) {
        this.negated = negated;
        this.val = val;
    }

    @Override
    public EnhIntPredicate or(IntPredicate other) {
        if (other instanceof SinglePredicate && !negated) {
            SinglePredicate sp = (SinglePredicate) other;
            if (!sp.negated) {
                if (sp.val == val) {
                    return this;
                }
                return IntPredicates.anyOf(val, sp.val);
            }
        }
        return EnhIntPredicate.super.or(other);
    }

    @Override
    public EnhIntPredicate and(IntPredicate other) {
        if (other instanceof SinglePredicate && !negated) {
            SinglePredicate sp = (SinglePredicate) other;
            if (!sp.negated) {
                if (sp.val == val) {
                    return this;
                }
                return FixedIntPredicate.INT_FALSE;
            }
        }
        return EnhIntPredicate.super.or(other);
    }

    @Override
    public boolean test(int value) {
        return negated ? value != val : value == val;
    }

    @Override
    public EnhIntPredicate negate() {
        return new SinglePredicate(!negated, val);
    }

    @Override
    public String toString() {
        String pfx = negated ? "!eq(" : "eq(";
        return pfx + val + ")";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Arrays.hashCode(new int[]{val});
        hash = 59 * hash + (this.negated ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof SinglePredicate) {
            return ((SinglePredicate) o).negated == negated
                     && ((SinglePredicate) o).val == val;
        } else if (o instanceof ArrayPredicate || o instanceof ArrayPredicateWithNames) {
            ArrayPredicate p = (ArrayPredicate) o;
            return p.vals.length == 1 && p.vals[0] == val && p.negated == negated;
        }
        return false;
    }
}
