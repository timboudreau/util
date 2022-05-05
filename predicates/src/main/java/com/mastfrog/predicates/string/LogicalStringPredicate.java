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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
final class LogicalStringPredicate implements EnhStringPredicate {

    private final boolean or;
    private final List<Predicate<? super String>> delegates = new ArrayList<>();

    LogicalStringPredicate(boolean or, Predicate<? super String> a, Predicate<? super String> b) {
        delegates.add(a);
        delegates.add(b);
        this.or = or;
    }

    LogicalStringPredicate(boolean or, List<Predicate<? super String>> all) {
        this.or = or;
        this.delegates.addAll(all);
    }

    LogicalStringPredicate copyAdding(Predicate<? super String> p) {
        LogicalStringPredicate result = new LogicalStringPredicate(or, delegates);
        result.delegates.add(p);
        return result;
    }

    @Override
    public boolean test(String t) {
        boolean result;
        if (or) {
            result = false;
            for (Predicate<? super String> test : delegates) {
                if (test.test(t)) {
                    result = true;
                    break;
                }
            }
        } else {
            result = true;
            for (Predicate<? super String> test : delegates) {
                if (!test.test(t)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public EnhStringPredicate or(Predicate<? super String> other) {
        if (or) {
            return copyAdding(other);
        }
        return EnhStringPredicate.super.or(other);
    }

    @Override
    public EnhStringPredicate and(Predicate<? super String> other) {
        if (!or) {
            return copyAdding(other);
        }
        return EnhStringPredicate.super.and(other);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (or) {
            sb.append("or(");
        } else {
            sb.append("and(");
        }
        for (Iterator<Predicate<? super String>> it = delegates.iterator(); it.hasNext();) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        return sb.append(')').toString();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (this.or ? 1 : 0);
        hash = 41 * hash + this.delegates.hashCode();
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LogicalStringPredicate other = (LogicalStringPredicate) obj;
        if (this.or != other.or) {
            return false;
        }
        return Objects.equals(this.delegates, other.delegates);
    }
}
