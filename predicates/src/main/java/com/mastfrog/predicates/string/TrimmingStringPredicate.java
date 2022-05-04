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
class TrimmingStringPredicate implements EnhStringPredicate {

    private final Predicate<String> delegate;

    public TrimmingStringPredicate(Predicate<String> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean test(String t) {
        if (t != null) {
            t = t.trim();
        }
        return delegate.test(t);
    }

    @Override
    public String toString() {
        return "trim(" + delegate + ")";
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || o.getClass() != TrimmingStringPredicate.class) {
            return false;
        }
        TrimmingStringPredicate other = (TrimmingStringPredicate) o;
        return delegate.equals(other.delegate);
    }
    
    @Override
    public int hashCode() {
        return delegate.hashCode() * 471;
    }

    @Override
    public EnhStringPredicate trimmingInput() {
        return this;
    }
}
