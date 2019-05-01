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
package com.mastfrog.function.throwing.io;

import com.mastfrog.function.throwing.*;
import java.io.IOException;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IOLongSupplier extends ThrowingLongSupplier {

    long getAsLong() throws IOException;

    @Override
    default IOLongSupplier or(LongPredicate test, LongSupplier next) {
        return () -> {
            long result = IOLongSupplier.this.getAsLong();
            return test.test(result) ? result : next.getAsLong();
        };
    }

    @Override
    default IOLongSupplier ifZero(LongSupplier next) {
        return () -> {
            long result = IOLongSupplier.this.getAsLong();
            return result == 0 ? next.getAsLong() : result;
        };
    }

    @Override
    default IOLongSupplier plus(LongSupplier next) {
        return () -> {
            return IOLongSupplier.this.getAsLong() + next.getAsLong();
        };
    }

    @Override
    default IOLongSupplier times(LongSupplier next) {
        return () -> {
            long result = IOLongSupplier.this.getAsLong();
            return result == 0 ? 0 : result * next.getAsLong();
        };
    }

    @Override
    default IOLongSupplier minus(LongSupplier next) {
        return () -> {
            return IOLongSupplier.this.getAsLong() - next.getAsLong();
        };
    }

    @Override
    default IOLongSupplier mod(LongSupplier next) {
        return () -> {
            return IOLongSupplier.this.getAsLong() % next.getAsLong();
        };
    }

    @Override
    default IOLongSupplier bitwiseOr(LongSupplier next) {
        return () -> {
            return IOLongSupplier.this.getAsLong() | next.getAsLong();
        };
    }

    @Override
    default IOLongSupplier bitwiseXor(LongSupplier next) {
        return () -> {
            return IOLongSupplier.this.getAsLong() ^ next.getAsLong();
        };
    }

    @Override
    default IOLongSupplier bitwiseAnd(LongSupplier next) {
        return () -> {
            return IOLongSupplier.this.getAsLong() & next.getAsLong();
        };
    }

    @Override
    default IOLongSupplier dividedBy(LongSupplier next) {
        return () -> {
            long result = IOLongSupplier.this.getAsLong();
            long div = next.getAsLong();
            return result == 0 || div == 0 ? 0 : result / div;
        };
    }

    default IOLongSupplier or(LongPredicate test, IOLongSupplier next) {
        return () -> {
            long result = IOLongSupplier.this.getAsLong();
            return test.test(result) ? result : next.getAsLong();
        };
    }

    default IOLongSupplier ifZero(IOLongSupplier next) {
        return () -> {
            long result = IOLongSupplier.this.getAsLong();
            return result == 0 ? next.getAsLong() : result;
        };
    }

    default IOLongSupplier plus(IOLongSupplier next) {
        return () -> {
            return IOLongSupplier.this.getAsLong() + next.getAsLong();
        };
    }

    default IOLongSupplier times(IOLongSupplier next) {
        return () -> {
            long result = IOLongSupplier.this.getAsLong();
            return result == 0 ? 0 : result * next.getAsLong();
        };
    }

    default IOLongSupplier minus(IOLongSupplier next) {
        return () -> {
            return IOLongSupplier.this.getAsLong() - next.getAsLong();
        };
    }

    default IOLongSupplier mod(IOLongSupplier next) {
        return () -> {
            return IOLongSupplier.this.getAsLong() % next.getAsLong();
        };
    }

    default IOLongSupplier bitwiseOr(IOLongSupplier next) {
        return () -> {
            return IOLongSupplier.this.getAsLong() | next.getAsLong();
        };
    }

    default IOLongSupplier bitwiseXor(IOLongSupplier next) {
        return () -> {
            return IOLongSupplier.this.getAsLong() ^ next.getAsLong();
        };
    }

    default IOLongSupplier bitwiseAnd(IOLongSupplier next) {
        return () -> {
            return IOLongSupplier.this.getAsLong() & next.getAsLong();
        };
    }

    default IOLongSupplier dividedBy(IOLongSupplier next) {
        return () -> {
            long result = IOLongSupplier.this.getAsLong();
            long div = next.getAsLong();
            return result == 0 || div == 0 ? 0 : result / div;
        };
    }

    default IOSupplier<Long> toBoxedSupplier() {
        return () -> {
            return getAsLong();
        };
    }

}
