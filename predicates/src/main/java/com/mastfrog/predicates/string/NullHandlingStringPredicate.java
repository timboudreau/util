/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.predicates.string;

import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
final class NullHandlingStringPredicate implements EnhStringPredicate {

    private final boolean onNull;
    private final Predicate<String> delegate;

    public NullHandlingStringPredicate(boolean onNull, Predicate<String> delegate) {
        this.onNull = onNull;
        this.delegate = delegate;
    }

    @Override
    public boolean test(String t) {
        if (t == null) {
            return onNull;
        }
        return delegate.test(t);
    }

    @Override
    public String toString() {
        return "nulls(" + onNull + ", " + delegate + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || o.getClass() != NullHandlingStringPredicate.class) {
            return false;
        }
        NullHandlingStringPredicate other = (NullHandlingStringPredicate) o;
        return other.onNull == onNull && delegate.equals(other.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode() * 94849 * (onNull ? 1 : -1);
    }

    @Override
    public EnhStringPredicate onNull(boolean passFail) {
        if (passFail == onNull) {
            return this;
        }
        return EnhStringPredicate.super.onNull(passFail);
    }
}
