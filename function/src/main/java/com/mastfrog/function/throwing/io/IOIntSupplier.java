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

import com.mastfrog.function.throwing.ThrowingIntSupplier;
import java.io.IOException;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IOIntSupplier extends ThrowingIntSupplier {

    @Override
    int getAsInt() throws IOException;

    @Override
    default IOIntSupplier or(IntPredicate test, IntSupplier next) {
        return () -> {
            int result = IOIntSupplier.this.getAsInt();
            return test.test(result) ? result : next.getAsInt();
        };
    }

    @Override
    default IOIntSupplier ifZero(IntSupplier next) {
        return () -> {
            int result = IOIntSupplier.this.getAsInt();
            return result == 0 ? next.getAsInt() : result;
        };
    }

    @Override
    default IOIntSupplier plus(IntSupplier next) {
        return () -> {
            return IOIntSupplier.this.getAsInt() + next.getAsInt();
        };
    }

    @Override
    default IOIntSupplier times(IntSupplier next) {
        return () -> {
            int result = IOIntSupplier.this.getAsInt();
            return result == 0 ? 0 : result * next.getAsInt();
        };
    }

    @Override
    default IOIntSupplier minus(IntSupplier next) {
        return () -> {
            return IOIntSupplier.this.getAsInt() - next.getAsInt();
        };
    }

    @Override
    default IOIntSupplier mod(IntSupplier next) {
        return () -> {
            return IOIntSupplier.this.getAsInt() % next.getAsInt();
        };
    }

    @Override
    default IOIntSupplier bitwiseOr(IntSupplier next) {
        return () -> {
            return IOIntSupplier.this.getAsInt() | next.getAsInt();
        };
    }

    @Override
    default IOIntSupplier bitwiseXor(IntSupplier next) {
        return () -> {
            return IOIntSupplier.this.getAsInt() ^ next.getAsInt();
        };
    }

    @Override
    default IOIntSupplier bitwiseAnd(IntSupplier next) {
        return () -> {
            return IOIntSupplier.this.getAsInt() & next.getAsInt();
        };
    }

    @Override
    default IOIntSupplier dividedBy(IntSupplier next) {
        return () -> {
            int result = IOIntSupplier.this.getAsInt();
            int div = next.getAsInt();
            return result == 0 || div == 0 ? 0 : result / div;
        };
    }

    default IOIntSupplier or(IntPredicate test, IOIntSupplier next) {
        return () -> {
            int result = IOIntSupplier.this.getAsInt();
            return test.test(result) ? result : next.getAsInt();
        };
    }

    default IOIntSupplier ifZero(IOIntSupplier next) {
        return () -> {
            int result = IOIntSupplier.this.getAsInt();
            return result == 0 ? next.getAsInt() : result;
        };
    }

    default IOIntSupplier plus(IOIntSupplier next) {
        return () -> {
            return IOIntSupplier.this.getAsInt() + next.getAsInt();
        };
    }

    default IOIntSupplier times(IOIntSupplier next) {
        return () -> {
            int result = IOIntSupplier.this.getAsInt();
            return result == 0 ? 0 : result * next.getAsInt();
        };
    }

    default IOIntSupplier minus(IOIntSupplier next) {
        return () -> {
            return IOIntSupplier.this.getAsInt() - next.getAsInt();
        };
    }

    default IOIntSupplier mod(IOIntSupplier next) {
        return () -> {
            return IOIntSupplier.this.getAsInt() % next.getAsInt();
        };
    }

    default IOIntSupplier bitwiseOr(IOIntSupplier next) {
        return () -> {
            return IOIntSupplier.this.getAsInt() | next.getAsInt();
        };
    }

    default IOIntSupplier bitwiseXor(IOIntSupplier next) {
        return () -> {
            return IOIntSupplier.this.getAsInt() ^ next.getAsInt();
        };
    }

    default IOIntSupplier bitwiseAnd(IOIntSupplier next) {
        return () -> {
            return IOIntSupplier.this.getAsInt() & next.getAsInt();
        };
    }

    default IOIntSupplier dividedBy(IOIntSupplier next) {
        return () -> {
            int result = IOIntSupplier.this.getAsInt();
            int div = next.getAsInt();
            return result == 0 || div == 0 ? 0 : result / div;
        };
    }
}
