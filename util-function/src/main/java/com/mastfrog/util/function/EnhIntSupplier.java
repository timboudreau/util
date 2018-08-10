/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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

package com.mastfrog.util.function;

import java.util.function.IntPredicate;
import java.util.function.IntSupplier;

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface EnhIntSupplier extends IntSupplier {

    default EnhIntSupplier or(IntPredicate test, EnhIntSupplier next) {
        return () -> {
            int result = EnhIntSupplier.this.getAsInt();
            return test.test(result) ? result : next.getAsInt();
        };
    }

    default EnhIntSupplier ifZero(EnhIntSupplier next) {
        return () -> {
            int result = EnhIntSupplier.this.getAsInt();
            return result == 0 ? next.getAsInt() : result;
        };
    }

    default EnhIntSupplier plus(EnhIntSupplier next) {
        return () -> {
            return EnhIntSupplier.this.getAsInt() + next.getAsInt();
        };
    }

    default EnhIntSupplier times(EnhIntSupplier next) {
        return () -> {
            int result = EnhIntSupplier.this.getAsInt();
            return result == 0 ? 0 : result * next.getAsInt();
        };
    }

    default EnhIntSupplier minus(EnhIntSupplier next) {
        return () -> {
            return EnhIntSupplier.this.getAsInt() - next.getAsInt();
        };
    }

    default EnhIntSupplier dividedBy(EnhIntSupplier next) {
        return () -> {
            int result = EnhIntSupplier.this.getAsInt();
            int div = next.getAsInt();
            return result == 0 || div == 0 ? 0 : result / div;
        };
    }
}
