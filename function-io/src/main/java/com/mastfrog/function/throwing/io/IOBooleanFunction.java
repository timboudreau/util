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
package com.mastfrog.function.throwing.io;

import com.mastfrog.function.throwing.*;
import java.io.IOException;

/**
 * A throwing function which takes a boolean and produces an object.
 *
 * @author Tim Boudreau
 * @param <T> The output type
 * @since 2.8.3.1
 */
@FunctionalInterface
public interface IOBooleanFunction<T> extends ThrowingBooleanFunction<T> {

    /**
     * Given an input value, return some object.
     *
     * @param val A true or false value
     * @return An object
     * @throws java.io.IOException if something goes wrong
     */
    @Override
    T applyAsBoolean(boolean val) throws IOException;

    /**
     * Return a BooleanFunction which returns the result of one of two suppliers
     * depending on the passed boolean value.
     *
     * @param <T> The type
     * @param falseSupplier
     * @param trueSupplier
     * @return
     */
    static <T> IOBooleanFunction<T> fromSuppliers(
            IOSupplier<T> falseSupplier,
            IOSupplier<T> trueSupplier) {
        return val -> val ? trueSupplier.get() : falseSupplier.get();
    }

    /**
     * Convert to an ordinary IOFunction, which treats null input as false.
     *
     * @return A function
     */
    @Override
    default IOFunction<Boolean, T> toFunction() {
        return val -> {
            boolean v = val == null ? false : val;
            return applyAsBoolean(v);
        };
    }
}
