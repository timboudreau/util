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

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
final class SubstringPredicate implements EnhStringPredicate {

    private final Relation mode;
    private final String target;

    public SubstringPredicate(Relation mode, String target) {
        this.mode = notNull("mode", mode);
        this.target = notNull("target", target);
    }

    @Override
    public boolean test(String t) {
        return mode.test(target, t);
    }

    @Override
    public String toString() {
        return mode.name().toLowerCase() + "(" + target + ")";
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + Objects.hashCode(this.mode);
        hash = 43 * hash + Objects.hashCode(this.target);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || obj.getClass() != SubstringPredicate.class) {
            return false;
        }
        final SubstringPredicate other = (SubstringPredicate) obj;
        return this.mode == other.mode
                && Objects.equals(this.target, other.target);
    }

    enum Relation {
        STARTS_WITH,
        ENDS_WITH,
        CONTAINS;

        boolean test(String target, String in) {
            if (in == null || target == null) {
                return false;
            }
            switch (this) {
                case STARTS_WITH:
                    return in.startsWith(target);
                case CONTAINS:
                    return in.contains(target);
                case ENDS_WITH:
                    return in.endsWith(target);
                default:
                    throw new AssertionError(this);
            }
        }

        String description(String target) {
            return name().toLowerCase() + '(' + target + ')';
        }
    }
}
