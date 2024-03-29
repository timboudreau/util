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
package com.mastfrog.predicates.string;

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
final class SingleStringPredicate implements EnhStringPredicate {

    private final boolean negated;
    private final String string;

    SingleStringPredicate(boolean negated, String string) {
        this.negated = negated;
        this.string = notNull("string", string);
    }

    @Override
    public boolean test(String t) {
        boolean result = string.equals(t);
        return negated ? !result : result;
    }

    public String toString() {
        return (negated ? "!equals(" : "equals(") + string + ")";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.negated ? 1 : 0);
        hash = 89 * hash + Objects.hashCode(this.string);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != SingleStringPredicate.class) {
            return false;
        }
        final SingleStringPredicate other = (SingleStringPredicate) obj;
        return this.negated == other.negated
                && this.string.equals(other.string);
    }
}
