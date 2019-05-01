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

import java.util.function.IntPredicate;
import java.util.function.IntSupplier;

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingIntSupplier {

    int getAsInt() throws Exception;

    default ThrowingIntSupplier or(IntPredicate test, IntSupplier next) {
        return () -> {
            int result = ThrowingIntSupplier.this.getAsInt();
            return test.test(result) ? result : next.getAsInt();
        };
    }

    default ThrowingIntSupplier ifZero(IntSupplier next) {
        return () -> {
            int result = ThrowingIntSupplier.this.getAsInt();
            return result == 0 ? next.getAsInt() : result;
        };
    }

    default ThrowingIntSupplier plus(IntSupplier next) {
        return () -> {
            return ThrowingIntSupplier.this.getAsInt() + next.getAsInt();
        };
    }

    default ThrowingIntSupplier times(IntSupplier next) {
        return () -> {
            int result = ThrowingIntSupplier.this.getAsInt();
            return result == 0 ? 0 : result * next.getAsInt();
        };
    }

    default ThrowingIntSupplier minus(IntSupplier next) {
        return () -> {
            return ThrowingIntSupplier.this.getAsInt() - next.getAsInt();
        };
    }

    default ThrowingIntSupplier mod(IntSupplier next) {
        return () -> {
            return ThrowingIntSupplier.this.getAsInt() % next.getAsInt();
        };
    }

    default ThrowingIntSupplier bitwiseOr(IntSupplier next) {
        return () -> {
            return ThrowingIntSupplier.this.getAsInt() | next.getAsInt();
        };
    }

    default ThrowingIntSupplier bitwiseXor(IntSupplier next) {
        return () -> {
            return ThrowingIntSupplier.this.getAsInt() ^ next.getAsInt();
        };
    }

    default ThrowingIntSupplier bitwiseAnd(IntSupplier next) {
        return () -> {
            return ThrowingIntSupplier.this.getAsInt() & next.getAsInt();
        };
    }

    default ThrowingIntSupplier dividedBy(IntSupplier next) {
        return () -> {
            int result = ThrowingIntSupplier.this.getAsInt();
            int div = next.getAsInt();
            return result == 0 || div == 0 ? 0 : result / div;
        };
    }

    default ThrowingIntSupplier or(IntPredicate test, ThrowingIntSupplier next) {
        return () -> {
            int result = ThrowingIntSupplier.this.getAsInt();
            return test.test(result) ? result : next.getAsInt();
        };
    }

    default ThrowingIntSupplier ifZero(ThrowingIntSupplier next) {
        return () -> {
            int result = ThrowingIntSupplier.this.getAsInt();
            return result == 0 ? next.getAsInt() : result;
        };
    }

    default ThrowingIntSupplier plus(ThrowingIntSupplier next) {
        return () -> {
            return ThrowingIntSupplier.this.getAsInt() + next.getAsInt();
        };
    }

    default ThrowingIntSupplier times(ThrowingIntSupplier next) {
        return () -> {
            int result = ThrowingIntSupplier.this.getAsInt();
            return result == 0 ? 0 : result * next.getAsInt();
        };
    }

    default ThrowingIntSupplier minus(ThrowingIntSupplier next) {
        return () -> {
            return ThrowingIntSupplier.this.getAsInt() - next.getAsInt();
        };
    }

    default ThrowingIntSupplier mod(ThrowingIntSupplier next) {
        return () -> {
            return ThrowingIntSupplier.this.getAsInt() % next.getAsInt();
        };
    }

    default ThrowingIntSupplier bitwiseOr(ThrowingIntSupplier next) {
        return () -> {
            return ThrowingIntSupplier.this.getAsInt() | next.getAsInt();
        };
    }

    default ThrowingIntSupplier bitwiseXor(ThrowingIntSupplier next) {
        return () -> {
            return ThrowingIntSupplier.this.getAsInt() ^ next.getAsInt();
        };
    }

    default ThrowingIntSupplier bitwiseAnd(ThrowingIntSupplier next) {
        return () -> {
            return ThrowingIntSupplier.this.getAsInt() & next.getAsInt();
        };
    }

    default ThrowingIntSupplier dividedBy(ThrowingIntSupplier next) {
        return () -> {
            int result = ThrowingIntSupplier.this.getAsInt();
            int div = next.getAsInt();
            return result == 0 || div == 0 ? 0 : result / div;
        };
    }

    default ThrowingSupplier<Integer> toBoxedSupplier() {
        return () -> {
            return getAsInt();
        };
    }

}
