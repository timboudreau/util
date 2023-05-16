/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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
package com.mastfrog.function;

import java.util.function.DoubleFunction;
import java.util.function.Supplier;

/**
 * A function which takes a float.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface FloatFunction<T> {

    /**
     * Accept a float and return an object of type T.
     *
     * @param val The value
     * @return The object
     */
    T apply(float val);

    /**
     * Returns a function which does not pass Float.NaN to this function, but
     * instead returns null.
     *
     * @return A float function
     */
    default FloatFunction<T> filterNaN() {
        return val -> {
            if (Float.isNaN(val)) {
                return null;
            }
            return apply(val);
        };
    }

    /**
     * Returns a function which does not pass non-finite values to this
     * function, but instead returns null.
     *
     * @return A float function
     */
    default FloatFunction<T> filterNonFinite() {
        return val -> {
            if (!Float.isFinite(val)) {
                return null;
            }
            return apply(val);
        };
    }

    /**
     * Wrap a FloatSupplier in this function to create a Supplier&lt;T&gt;.
     *
     * @param supp A supplier of floats
     * @return A supplier of objects
     */
    default Supplier<T> toSupplier(FloatSupplier supp) {
        return () -> {
            return apply(supp.getAsFloat());
        };
    }

    /**
     * Convert this function to one which takes doubles and throws an
     * exception if the passed value is out of range for floats.
     *
     * @return A double-accepting function which delegates to this one
     */
    default DoubleFunction<T> toDoubleFunction() {
        return dbl -> {
            if (dbl > Float.MAX_VALUE || dbl < Float.MIN_VALUE) {
                throw new IllegalArgumentException("Value out of range for float: " + dbl);
            }
            return apply((float) dbl);
        };
    }

    /**
     * Create a FloatFunction from a DoubleFunction.
     *
     * @param <T> The function return type
     * @param df A double function
     * @return A float function
     */
    static <T> FloatFunction<T> fromDoubleFunction(DoubleFunction<T> df) {
        return val -> {
            return df.apply(val);
        };
    }
}
