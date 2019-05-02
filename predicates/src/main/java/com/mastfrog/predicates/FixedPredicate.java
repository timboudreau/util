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
package com.mastfrog.predicates;

import com.mastfrog.function.BytePredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
final class FixedPredicate implements Predicate<Object>, NamedPredicate<Object> {

    static final LongPredicate LONG_FALSE, LONG_TRUE;
    static final BytePredicate BYTE_FALSE, BYTE_TRUE;
    static final NamedPredicate<Object> FALSE;
    static final NamedPredicate<Object> TRUE;

    static {
        LONG_FALSE = new FixedLongPredicate(false);
        LONG_TRUE = new FixedLongPredicate(true);
        BYTE_FALSE = new FixedBytePredicate(false);
        BYTE_TRUE = new FixedBytePredicate(true);
        FALSE = new FixedPredicate(false);
        TRUE = new FixedPredicate(true);
    }

    private final boolean value;

    FixedPredicate(boolean value) {
        this.value = value;
    }

    @Override
    public boolean test(Object t) {
        return value;
    }

    @Override
    public int hashCode() {
        return value ? 47 : 103;
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
        final FixedPredicate other = (FixedPredicate) obj;
        if (this.value != other.value) {
            return false;
        }
        return true;
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

    @Override
    public String name() {
        return stringValue(value);
    }

    @Override
    public NamedPredicate<Object> negate() {
        if (this == TRUE) {
            return FALSE;
        } else {
            return TRUE;
        }
    }

    @Override
    public NamedPredicate<Object> or(Predicate<? super Object> other) {
        if (this == TRUE) {
            return this;
        } else {
            return other instanceof NamedPredicate ? (NamedPredicate<Object>) other :
                    new NamedWrapperPredicate<>(other.toString(), other);
        }
    }

    @Override
    public NamedPredicate<Object> and(Predicate<? super Object> other) {
        if (this == TRUE) {
            return NamedPredicate.super.and(other);
        } else {
            return this;
        }
    }

    static final class FixedBytePredicate implements BytePredicate {

        private final boolean value;

        public FixedBytePredicate(boolean value) {
            this.value = value;
        }

        @Override
        public boolean test(byte val) {
            return value;
        }

        @Override
        public BytePredicate and(BytePredicate other) {
            return value ? other : this;
        }

        @Override
        public BytePredicate negate() {
            return this == BYTE_TRUE ? BYTE_FALSE : BYTE_TRUE;
        }

        @Override
        public BytePredicate or(BytePredicate other) {
            return value ? this : other;
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object o) {
            return (this == BYTE_FALSE && o == BYTE_FALSE)
                    || (this == BYTE_TRUE && o == BYTE_TRUE);
        }

        @Override
        public int hashCode() {
            return FixedPredicate.hashCode(value);
        }

        public String toString() {
            return FixedPredicate.stringValue(value);
        }
    }


    static final class FixedLongPredicate implements LongPredicate {

        private final boolean value;

        FixedLongPredicate(boolean value) {
            this.value = value;
        }

        @Override
        public boolean test(long val) {
            return value;
        }

        @Override
        public LongPredicate and(LongPredicate other) {
            return value ? other : this;
        }

        @Override
        public LongPredicate negate() {
            return this == LONG_TRUE ? LONG_FALSE : LONG_TRUE;
        }

        @Override
        public LongPredicate or(LongPredicate other) {
            return value ? this : other;
        }

        @Override
        public boolean equals(Object o) {
            return (this == LONG_FALSE && o == LONG_FALSE)
                    || (this == LONG_TRUE && o == LONG_TRUE);
        }

        @Override
        public int hashCode() {
            return FixedPredicate.hashCode(value);
        }

        public String toString() {
            return FixedPredicate.stringValue(value);
        }
    }
}
