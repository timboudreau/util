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
package com.mastfrog.function.throwing;

import com.mastfrog.util.preconditions.Exceptions;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingLongSupplier {

    long getAsLong() throws Exception;

    /**
     * Convert to a non-throwing equivalent. Note that the resulting method
     * <i>will</i> rethrow any thrown checked exceptions.
     *
     * @return An equivalent function that does not declare the exceptions which
     * it throws
     */
    default LongSupplier toNonThrowing() {
        return () -> {
            try {
                return getAsLong();
            } catch (Exception ex) {
                return Exceptions.chuck(ex);
            }
        };
    }

    default ThrowingLongSupplier or(LongPredicate test, LongSupplier next) {
        return () -> {
            long result = ThrowingLongSupplier.this.getAsLong();
            return test.test(result) ? result : next.getAsLong();
        };
    }

    default ThrowingLongSupplier ifZero(LongSupplier next) {
        return () -> {
            long result = ThrowingLongSupplier.this.getAsLong();
            return result == 0 ? next.getAsLong() : result;
        };
    }

    default ThrowingLongSupplier plus(LongSupplier next) {
        return () -> {
            return ThrowingLongSupplier.this.getAsLong() + next.getAsLong();
        };
    }

    default ThrowingLongSupplier times(LongSupplier next) {
        return () -> {
            long result = ThrowingLongSupplier.this.getAsLong();
            return result == 0 ? 0 : result * next.getAsLong();
        };
    }

    default ThrowingLongSupplier minus(LongSupplier next) {
        return () -> {
            return ThrowingLongSupplier.this.getAsLong() - next.getAsLong();
        };
    }

    default ThrowingLongSupplier mod(LongSupplier next) {
        return () -> {
            return ThrowingLongSupplier.this.getAsLong() % next.getAsLong();
        };
    }

    default ThrowingLongSupplier bitwiseOr(LongSupplier next) {
        return () -> {
            return ThrowingLongSupplier.this.getAsLong() | next.getAsLong();
        };
    }

    default ThrowingLongSupplier bitwiseXor(LongSupplier next) {
        return () -> {
            return ThrowingLongSupplier.this.getAsLong() ^ next.getAsLong();
        };
    }

    default ThrowingLongSupplier bitwiseAnd(LongSupplier next) {
        return () -> {
            return ThrowingLongSupplier.this.getAsLong() & next.getAsLong();
        };
    }

    default ThrowingLongSupplier dividedBy(LongSupplier next) {
        return () -> {
            long result = ThrowingLongSupplier.this.getAsLong();
            long div = next.getAsLong();
            return result == 0 || div == 0 ? 0 : result / div;
        };
    }

    default ThrowingLongSupplier or(LongPredicate test, ThrowingLongSupplier next) {
        return () -> {
            long result = ThrowingLongSupplier.this.getAsLong();
            return test.test(result) ? result : next.getAsLong();
        };
    }

    default ThrowingLongSupplier ifZero(ThrowingLongSupplier next) {
        return () -> {
            long result = ThrowingLongSupplier.this.getAsLong();
            return result == 0 ? next.getAsLong() : result;
        };
    }

    default ThrowingLongSupplier plus(ThrowingLongSupplier next) {
        return () -> {
            return ThrowingLongSupplier.this.getAsLong() + next.getAsLong();
        };
    }

    default ThrowingLongSupplier times(ThrowingLongSupplier next) {
        return () -> {
            long result = ThrowingLongSupplier.this.getAsLong();
            return result == 0 ? 0 : result * next.getAsLong();
        };
    }

    default ThrowingLongSupplier minus(ThrowingLongSupplier next) {
        return () -> {
            return ThrowingLongSupplier.this.getAsLong() - next.getAsLong();
        };
    }

    default ThrowingLongSupplier mod(ThrowingLongSupplier next) {
        return () -> {
            return ThrowingLongSupplier.this.getAsLong() % next.getAsLong();
        };
    }

    default ThrowingLongSupplier bitwiseOr(ThrowingLongSupplier next) {
        return () -> {
            return ThrowingLongSupplier.this.getAsLong() | next.getAsLong();
        };
    }

    default ThrowingLongSupplier bitwiseXor(ThrowingLongSupplier next) {
        return () -> {
            return ThrowingLongSupplier.this.getAsLong() ^ next.getAsLong();
        };
    }

    default ThrowingLongSupplier bitwiseAnd(ThrowingLongSupplier next) {
        return () -> {
            return ThrowingLongSupplier.this.getAsLong() & next.getAsLong();
        };
    }

    default ThrowingLongSupplier dividedBy(ThrowingLongSupplier next) {
        return () -> {
            long result = ThrowingLongSupplier.this.getAsLong();
            long div = next.getAsLong();
            return result == 0 || div == 0 ? 0 : result / div;
        };
    }

    default ThrowingSupplier<Long> toBoxedSupplier() {
        return () -> {
            return getAsLong();
        };
    }
}
